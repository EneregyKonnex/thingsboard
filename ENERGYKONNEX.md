# EnergyKonnex — ThingsBoard Fork

This repository is a fork of [thingsboard/thingsboard](https://github.com/thingsboard/thingsboard) with EnergyKonnex-specific branding and build customisations.

## Repository setup

| Remote     | URL                                              |
|------------|--------------------------------------------------|
| `origin`   | `git@github.com:EneregyKonnex/thingsboard.git`  |
| `upstream` | `git@github.com:thingsboard/thingsboard.git`    |

## Branching strategy

```
upstream/release-X.Y          (ThingsBoard release branch)
        │
        └── origin/release-X.Y        (synced copy in our fork)
                │
                └── origin/energykonnex-X.Y   (our branding on top)
                        ├── commit: Update email templates and messages to reflect EnergyKonnex branding
                        ├── commit: EnergyKonnex branding (UI: logo, favicon, environment files)
                        ├── commit: Update README.md to include build instructions
                        └── commit: Update README.md and build.sh with simplified build commands
```

For every ThingsBoard release we track, there is a matching `energykonnex-X.Y` branch that sits on top of `release-X.Y` with a small, consistent set of branding commits.

### Existing branches

| EnergyKonnex branch   | Based on       |
|------------------------|----------------|
| `energykonnex-3.9`    | `release-3.9`  |
| `energykonnex-4.0`    | `release-4.0`  |
| `energykonnex-4.2`    | `release-4.2`  |
| `energykonnex-4.3`    | `release-4.3`  |

## What the branding commits change

1. **Email templates & i18n** — replaces ThingsBoard references with EnergyKonnex in email templates (`application/src/main/resources/templates/*.ftl`) and `messages.properties`.
2. **UI branding** — custom logo (`logo_title_white.png`), favicon, environment config (`appTitle`, `logoUrl`), home component tweaks.
3. **README** — adds build instructions for common build targets.
4. **build.sh** — uses `-T0.8C` parallelism and enables `-Ddockerfile.skip=false` by default, with a simplified `./build.sh <projects>` interface.

## Creating a new energykonnex branch for a new release

When ThingsBoard publishes a new release (e.g. `release-X.Y`):

```bash
# 1. Fetch the latest from both remotes
git fetch upstream
git fetch origin

# 2. Make sure origin has the release branch
#    (push upstream's release branch to our fork if not already there)
git push origin upstream/release-X.Y:refs/heads/release-X.Y

# 3. Create the energykonnex branch from release-X.Y
git checkout -b energykonnex-X.Y origin/release-X.Y

# 4. Cherry-pick the branding commits from the previous energykonnex branch
#    (check which commits to pick with: git log --oneline origin/release-A.B..origin/energykonnex-A.B)
git cherry-pick <commit-hash-1> <commit-hash-2> <commit-hash-3> <commit-hash-4>

# 5. Resolve any conflicts (typically only in README.md due to upstream changes)
#    Then push:
git push -u origin energykonnex-X.Y
```

## Rebasing when upstream release-X.Y gets fixes

When the upstream `release-X.Y` branch receives hotfixes or patches:

```bash
# 1. Fetch the latest
git fetch upstream

# 2. Update our copy of the release branch
git push origin upstream/release-X.Y:refs/heads/release-X.Y

# 3. Rebase our branding commits on top of the updated release branch
git checkout energykonnex-X.Y
git rebase origin/release-X.Y

# 4. Resolve any conflicts if needed, then force-push
#    (safe because only our 4 branding commits are rebased)
git push --force-with-lease origin energykonnex-X.Y
```

> **Note:** `--force-with-lease` is used instead of `--force` to prevent overwriting
> changes pushed by others. Since only the 4 branding commits sit on top, the
> rebase is low-risk.

## Building

```bash
# Build everything
mvn -T 0.8C license:format clean install -DskipTests -Ddockerfile.skip=false

# Or use the helper script
./build.sh                              # build all
./build.sh msa/web-ui                   # build web UI only
./build.sh msa/web-ui,msa/tb-node      # build web UI + tb-node
```
