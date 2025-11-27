<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# `goto` Changelog

## [Unreleased]

## [0.1.1]

### Changed

- Made the plugin dynamic so it can be installed/updated without an IDE restart

## [0.1.0]

### Added

- Initial release
- Go to Line with live preview
- Support for line and column navigation (e.g., `42`, `42:10`)
- Relative line navigation with `j`/`k` (e.g., `10j`, `5k`)
- Visual line navigation with `gj`/`gk` for soft-wrapped lines
- Selection extension when navigating with existing selection
- Configurable preview debounce delay
