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
// Check string is null
function isNullString(string) {
  return (
    !string ||
    string.length === 0 ||
    string === "null" ||
    string === "undefined"
  );
}

/**
 * @param {string} string
 * @returns {string | null}
 */
function orNullString(string) {
  return isNullString(string) ? null : string;
}

/** @type {string} */
const tagFormat = core.getInput("tag_format", { required: false }) ?? "$1";
const tagFormatRegex = new RegExp(tagFormat.replaceAll("$1", "(.+)"));

const prerelease = core.getInput("prerelease", { required: false }) === "true";

// If there is no previous tag, Then the initial tag will be used
function initialTag(tag) {
  const version = semver.parse(tag);
  const suffix = core.getInput("prerelease_suffix");
  if (prerelease) version.prerelease = [suffix, 0];
  return version.format();
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
  return semver.inc(semTag, bumpType, preName);
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
        core.info(
          `Computing semantic version, increasing ${type}; suffix=${preName}`,
        );
        return semver.inc(semTag, type, preName);
      default:
        core.setFailed(
          `Unsupported semantic version type ${type}. Must be one of (${Object.values(Semantic).join(", ")})`,
        );
    }
  } catch (error) {
    core.setFailed(`Failed to compute next semantic tag: ${error}`);
  }
  return null;
}

async function computeLastTag() {
  const recentTags = await existingTags();
  const tagNames = recentTags
    .map((tag) => tag.ref.replace("refs/tags/", ""))
    .filter((name) => name.match(tagFormatRegex));
  core.info(`recentTags (first 10): ${tagNames.slice(0, 10).join(", ")}`);

  return tagNames.shift();
}

async function computeNextTag(scheme, lastTag) {
  const minimum = orNullString(core.getInput("minimum_version"));

  // Handle zero-state where no tags exist for the repo
  if (!lastTag) {
    core.info(`Creating initial tag on ${scheme} scheme, minimum=${minimum}`);
    if (scheme === Scheme.Continuous) {
      return initialTag(minimum ?? "1");
    }
    return initialTag(minimum ?? "0.1.0");
  }
  core.info(`Computing the next tag based on: ${lastTag}`);
  core.setOutput("previous_tag", lastTag);

  const semTag = semver.parse(tagFormatRegex.exec(lastTag)[1]);

  if (semTag == null) {
    core.setFailed(`Failed to parse tag: ${lastTag}`);
    return null;
  }

  if (minimum != null) {
    const minimumVersion = semver.parse(minimum);
    if (semver.compare(minimumVersion, semTag) < 0) {
      if (
        minimumVersion.major == semTag.major &&
        minimumVersion.minor == semTag.minor &&
        minimumVersion.patch == semTag.patch &&
        minimumVersion.build.every((v, i) => v == semTag.build.at(i)) &&
        minimumVersion.prerelease.length == 0 &&
        semTag.prerelease.length > 0
      ) {
        // special case where minimum=1.0.0, last=1.0.0-build.n -> allows this
      } else {
        return initialTag(minimum);
      }
    }
  }

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

async function getDiff(lastTag, currentRef) {
  let result = "";
  const raw = await octokit.rest.repos.compareCommitsWithBasehead({
    owner,
    repo,
    basehead: `${lastTag}...${currentRef}`,
  });

  const commits = raw.data.commits;
  commits.reverse();

  const template =
    orNullString(core.getInput("diff_template")) ??
    "{{title}} ([{{commitHashAbbr}}]({{{commitUrl}}}))";
  for (const entry of commits) {
    const commit = entry.commit;
    const context = {
      commitHash: entry.sha,
      commitHashAbbr: entry.sha.slice(0, 6),
      commitUrl: entry.html_url,
      authorName: commit.author.name,
      authorEmail: commit.author.email,
      authorDate: commit.author.date,
      committerDate: commit.committer.date,
      parentHash: entry.parents?.[0].sha,
      body: commit.message,
      title: commit.message.slice(
        0,
        commit.message.includes("\n")
          ? commit.message.indexOf("\n")
          : undefined,
      ),
    };
    result += "\n- " + Mustache.render(template, context);
  }
  result += `\n\n**Full Changelog**: ${raw.data.html_url}`
  return result;
}

async function run() {
  try {
    const tagName = core.getInput("tag_name", { required: false });
    const scheme = core.getInput("tag_schema", { required: false });
    if (scheme !== Scheme.Continuous && scheme !== Scheme.Semantic) {
      core.setFailed(`Unsupported version scheme: ${scheme}`);
      return;
    }
    // Use predefined tag or calculate automatic next tag
    const releaseInfo =
      "lhwdev_create_release_info" in process.env
        ? JSON.parse(process.env["lhwdev_create_release_info"])
        : null;
    const lastTag = releaseInfo ? releaseInfo.lastTag : await computeLastTag();
    let version, tag;

    if (isNullString(tagName)) {
      if (releaseInfo != null) {
        version = releaseInfo.version;
        tag = releaseInfo.tag;
      } else {
        version = await computeNextTag(scheme, lastTag);
        tag = tagFormat.replaceAll("$1", version);
      }
    } else {
      tag = tagName.replace("refs/tags/", "");
      version = tagFormatRegex.exec(tag)[1];
    }

    if (releaseInfo) {
      core.info(`Reused tag from previous run: ${tag}`);
    } else {
      core.info(`Computed the next tag: ${tag}`);
    }

    const ctx = { lastTag, version, tag };
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
      bodyInput.includes("diff") && lastTag ? await getDiff(lastTag, ref) : "";

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
      `Created Github release ${createReleaseResponse.data.id} in ${createReleaseResponse.data.html_url}`,
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
          },
        );
        core.info(`Uploaded file ${path}, id=${uploadReleaseResult.data.id}`);
      }
    }
  } catch (error) {
    core.setFailed(error.message);
  }
}

module.exports = run;
