# Contributing


## Workflow

If you want to add a feature but are not sure how to do this or how it should behave in edge cases, feel free to chat us up in [Discord](https://discord.gg/JTrUxhq7sj). 

When making PRs, please allow Airstream maintainers to make changes to your branch. We might make changes, so make a copy of your branch if you need it.


## Code style

Please try to match the existing code style, including naming conventions and formatting. I might fix up such things to my liking before merging.

We don't have scalafmt set up at the moment. It's just not flexible enough to support my preferred style.


## N-Generators

Airstream offers several types of methods like `combineWith` and `mapN` in varying arity. These live in packages called `generated`, in implicit classes that are generated at compile time by generators located in the `project` folder.

To apply and execute your changes to the generators, run `reload; compile` in sbt. We commit all generated files to git because invisible code is annoying to figure out, and to help with source maps.


# Documentation

README.md needs to be updated before the changes can be merged into `master`. I usually do this after the feature is done.

We used to publish itemized changes in CHANGELOG.md, but we switched to publishing release blog posts at [laminar.dev](https://laminar.dev).
