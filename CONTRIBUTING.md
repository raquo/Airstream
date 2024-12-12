# Contributing


## Workflow

If you want to add a feature but are not sure how to do this or how it should behave in edge cases, feel free to chat us up in [Discord](https://discord.gg/JTrUxhq7sj). 

When making PRs, please allow Airstream maintainers to make changes to your branch. We might make changes, so make a copy of your branch if you need it.


## Code style

Please run `sbt scalafmtAll` before submitting the PR.

This only sets the ground rules, so please try to maintain the general style of the codebase.


## Tests

Please run `sbt +test` locally before submitting the PR.

Note that existing tests print this compiler warning in Scala 3:
- [E029] Pattern Match Exhaustivity Warning: .../src/test/scala-3/com/raquo/airstream/split/SplitMatchOneSpec.scala

This is expected. Ideally I would assert that this warning exists instead of printing it, but I don't think that's possible. I don't want to hide such warnings wholesale, but suggestions for improvement are welcome.


## N-Generators

Airstream offers several types of methods like `combineWith` and `mapN` in varying arity. These live in packages called `generated`, in implicit classes that are generated at compile time by generators located in the `project` folder.

To apply and execute your changes to the generators, run `reload; compile` in sbt. We commit all generated files to git because invisible code is annoying to figure out, and to help with source maps.


# Documentation

README.md needs to be updated before the changes can be merged into `master`. I usually do this after the feature is done.

We used to publish itemized changes in CHANGELOG.md, but we switched to publishing release blog posts at [laminar.dev](https://laminar.dev).
