const core = require("@actions/core");
const run = require("./create-release");

if (require.main === module) {
  try {
    run();
  } catch (e) {
    core.error(e);
    core.setFailed(e instanceof Error ? e.message : `${e}`);
  }
}
