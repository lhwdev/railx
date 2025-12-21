const core = require("@actions/core");
const { getExecOutput } = require("@actions/exec");
const github = require("@actions/github");

const context = github.context;
const { owner, repo } = context.repo;
const semver = require("semver");
const process = require("process");
const fs = require("fs");
const Mustache = require("mustache");
const { glob } = require("glob");

const octokit = github.getOctokit(core.getInput("github_token"));
const Scheme = {
  Continuous: "continuous",
  Semantic: "semantic",
};
const Semantic = {
  Major: "major",
  Minor: "minor",
  Patch: "patch",
  Premajor: "premajor",
  Prerelease: "prerelease",
};
const prerelease = core.getInput("prerelease", { required: false }) === "true";

// Check string is null
function isNullString(string) {
  return (
    !string ||
    string.length === 0 ||
    string === "null" ||
    string === "undefined"
  );
}

// If there is no previous tag, Then the initial tag will be used
function initialTag(tag) {
  const suffix = core.getInput("prerelease_suffix");
  const newTag = prerelease ? `${tag}-${suffix}.0` : tag;

  return newTag;
}

async function existingTags() {
  const { data: refs } = await octokit.rest.git.listMatchingRefs({
    owner,
    repo,
    ref: "tags",
  });

  // Sort tags by semantic version in descending order (highest first)
  return refs.sort((a, b) => {
    const tagA = a.ref.replace("refs/tags/", "");
    const tagB = b.ref.replace("refs/tags/", "");

    // Try to parse as semantic versions
    const versionA = semver.coerce(tagA);
    const versionB = semver.coerce(tagB);

    // If both are valid semantic versions, compare them
    if (versionA && versionB) {
      return semver.rcompare(versionA, versionB); // reverse compare for descending order
    }

    // If one or both are not valid semantic versions, fall back to string comparison
    if (!versionA && !versionB) {
      return tagB.localeCompare(tagA); // reverse for descending order
    }

    // Put valid semantic versions before invalid ones
    if (versionA && !versionB) return -1;
    if (!versionA && versionB) return 1;

    return 0;
  });
}

function semanticVersion(tag) {
  try {
    const [version, pre] = tag.split("-", 2);
    const sem = semver.parse(semver.coerce(version));

    if (!isNullString(pre)) {
      sem.prerelease = semver.prerelease(`0.0.0-${pre}`);
    }

    return sem;
  } catch (_) {
    // semver will return null if it fails to parse, maintain this behavior in our API
    return null;
  }
}

function determineContinuousBumpType(semTag) {
  const type = core.getInput("auto_increment_type") || Semantic.Major;
  const hasExistingPrerelease = semTag.prerelease.length > 0;

  switch (type) {
    case Semantic.Prerelease:
      return hasExistingPrerelease ? Semantic.Prerelease : Semantic.Premajor;
    case Semantic.Premajor:
      return Semantic.Premajor;
    default:
      return Semantic.Major;
  }
}

function determinePrereleaseName(semTag) {
  if (semTag.prerelease.length > 0) {
    return semTag.prerelease[0];
  }
  return core.getInput("prerelease_suffix") || "beta";
}

function computeNextContinuous(semTag) {
  const bumpType = determineContinuousBumpType(semTag);
  const preName = determinePrereleaseName(semTag);
  const nextSemTag = semver.parse(semver.inc(semTag, bumpType, preName));
  const tagSuffix =
    nextSemTag.prerelease.length > 0
      ? `-${nextSemTag.prerelease.join(".")}`
      : "";
  return [semTag.options.tagPrefix, nextSemTag.major, tagSuffix].join("");
}

function computeNextSemantic(semTag) {
  try {
    const type = core.getInput("auto_increment_type") || Semantic.Patch;
    const preName = determinePrereleaseName(semTag);

    switch (type) {
      case Semantic.Major:
      case Semantic.Minor:
      case Semantic.Patch:
      case Semantic.Premajor:
      case Semantic.Prerelease:
        return `${semTag.options.tagPrefix}${semver.inc(semTag, type, preName)}`;
      default:
        core.setFailed(
          `Unsupported semantic version type ${type}. Must be one of (${Object.values(Semantic).join(", ")})`
        );
    }
  } catch (error) {
    core.setFailed(`Failed to compute next semantic tag: ${error}`);
  }
  return null;
}

async function computeLastTag() {
  const recentTags = await existingTags();
  const tagNames = recentTags.map((tag) => tag.ref.replace("refs/tags/", ""));
  core.info(`recentTags (first 10): ${tagNames.slice(0, 10).join(", ")}`);
  if (recentTags.length < 1) {
    return null;
  }
  return recentTags.shift().ref.replace("refs/tags/", "");
}

async function computeNextTag(scheme, lastTag) {
  // Handle zero-state where no tags exist for the repo
  if (!lastTag) {
    if (scheme === Scheme.Continuous) {
      return initialTag("v1");
    }
    return initialTag("v0.1.0");
  }
  core.info(`Computing the next tag based on: ${lastTag}`);
  core.setOutput("previous_tag", lastTag);

  const semTag = semanticVersion(lastTag);

  if (semTag == null) {
    core.setFailed(`Failed to parse tag: ${lastTag}`);
    return null;
  }
  semTag.options.tagPrefix = lastTag.startsWith("v") ? "v" : "";

  if (scheme === Scheme.Continuous) {
    return computeNextContinuous(semTag);
  }
  return computeNextSemantic(semTag);
}

/** @param {string} str */
function processTemplate(str, ctx) {
  const env = new Mustache.Context(process.env);
  const context = new Mustache.Context(ctx, env);
  return Mustache.render(str, context);
}

async function getDiff(lastTag) {
  const result = [];
  const raw = await getExecOutput("git", [
    "log",
    `${lastTag}..HEAD`,
    "--format=%H%n%aN%n%aE%n%at%n%ct%n%P%n%D%n%B", // commit hash, author name, author email, author date, committer date, parent hash, ref name, raw body
    "-z", // null separator
    "--diff-merges=first-parent",
  ]);
  let template = core.getInput("diff_template");
  if (isNullString(template))
    template =
      "{{commitHashAbbr}} {{title}} [{{commitHashAbbr}}]({{commitUrl}})";
  for (const entry of raw.stdout.split("\0")) {
    const [
      commitHash,
      authorName,
      authorEmail,
      authorDate,
      committerDate,
      parentHash,
      refName,
      ...body
    ] = entry.split("\n");
    const context = {
      commitHash,
      commitHashAbbr: commitHash.slice(0, 6),
      commitUrl: `https://github.com/${owner}/${repo}/commit/${commitHash}`,
      authorName,
      authorEmail,
      authorDate,
      committerDate,
      parentHash,
      refName,
      body: body.join("\n"),
      title: body[0],
    };
    result.push(Mustache.render(template, context));
  }
  return result;
}

async function run() {
  try {
    // Get the inputs from the workflow file: https://github.com/actions/toolkit/tree/master/packages/core#inputsoutputs
    const tagName = core.getInput("tag_name", { required: false });
    const scheme = core.getInput("tag_schema", { required: false });
    if (scheme !== Scheme.Continuous && scheme !== Scheme.Semantic) {
      core.setFailed(`Unsupported version scheme: ${scheme}`);
      return;
    }
    // Use predefined tag or calculate automatic next tag
    const releaseInfo =
      "lhwdev_create_release_info" in process.env
        ? JSON.parse(process.env["lhwdev_create_release_info" in process.env])
        : null;
    const lastTag = releaseInfo?.lastTag ?? (await computeLastTag());
    const tag = isNullString(tagName)
      ? (releaseInfo?.tag ?? (await computeNextTag(scheme, lastTag)))
      : tagName.replace("refs/tags/", "");
    if ("lhwdev_create_release_info" in process.env) {
      core.info(`Reused tag from previous run: ${tag}`);
    } else {
      core.info(`Computed the next tag: ${tag}`);
    }

    const version = tag.startsWith("v") ? tag.slice(1) : tag;
    const ctx = { lastTag, tag, version };
    if (core.getInput("dry_run") && core.getBooleanInput("dry_run")) {
      core.setOutput("current_tag", tag);
      core.setOutput("version", version);
      core.exportVariable("lhwdev_create_release_info", ctx);
      return;
    }

    const releaseName = core.getInput("release_name", { required: false });
    const release = isNullString(releaseName)
      ? tag
      : processTemplate(releaseName.replace("refs/tags/", ""), ctx);
    ctx.release = release;

    const draft = core.getInput("draft", { required: false }) === "true";
    ctx.draft = draft;

    let ref = core.getInput("ref");
    if (isNullString(ref)) {
      const output = await getExecOutput("git", [
        "rev-parse",
        "--abbrev-ref",
        "HEAD",
      ]);
      ref = output.stdout.trim();
      core.info(`Defaulting to ref ${ref}`);
    }

    const bodyInput = core.getInput("body", { required: false });
    ctx.diff =
      bodyInput.includes("diff") && lastTag
        ? (await getDiff(lastTag)).map((line) => `- ${line}`).join("\n")
        : "";

    const body = processTemplate(bodyInput, ctx);

    // Create a release
    // API Documentation: https://developer.github.com/v3/repos/releases/#create-a-release
    // Octokit Documentation: https://octokit.github.io/rest.js/#octokit-routes-repos-create-release
    const createReleaseResponse = await octokit.rest.repos.createRelease({
      owner,
      repo,
      target_commitish: ref,
      tag_name: tag,
      name: release,
      body,
      draft,
      prerelease,
    });

    core.info(
      `Created Github release ${createReleaseResponse.data.id} in ${createReleaseResponse.data.html_url}`
    );

    // Get the ID, html_url, and upload URL for the created Release from the response
    const {
      data: { id: releaseId, html_url: htmlUrl, upload_url: uploadUrl },
    } = createReleaseResponse;

    // Set the output variables for use by other actions: https://github.com/actions/toolkit/tree/master/packages/core#inputsoutputs
    core.setOutput("current_tag", tag);
    core.setOutput("version", version);
    core.setOutput("id", releaseId);
    core.setOutput("html_url", htmlUrl);
    core.setOutput("upload_url", uploadUrl);

    const artifacts = core.getMultilineInput("artifacts", { required: false });
    if (artifacts.length != 0 && artifacts[0].length != 0) {
      const files = await glob(artifacts, { absolute: false });
      for (const path of files) {
        const uploadReleaseResult = await octokit.rest.repos.uploadReleaseAsset(
          {
            owner,
            repo,
            release_id: releaseId,
            name: path.slice(path.lastIndexOf("/") + 1),
            data: fs.readFileSync(path),
          }
        );
        core.info(`Uploaded file ${path}, id=${uploadReleaseResult.data.id}`);
      }
    }
  } catch (error) {
    core.setFailed(error.message);
  }
}

module.exports = run;
