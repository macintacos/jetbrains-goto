# jetbrains-goto

![Build](https://github.com/macintacos/goto/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

<!-- Plugin description -->
Currently provides a single command, "Go to Line (Preview)", which gives you a proper preview for
navigating to a line in the current file.

Featureset:

- Command "Go to Line (Preview)" provided. If you're using IdeaVIM, the command is called
  `GoToLinePreview` if you want to remap it.
- Ability to remap the command in the UI itself.
- Support a variety of formats for input:
    - `line` - just provide a number (e.g. `10`) and it will bring you to that line in the file.
    - `line:column` - will do the same, but also jump to the column you specify.
    - `line[j,k]` - for relative line jumping. Type `10j` for example, and you will jump down 10
      lines from the current position.
    - `line[gj,gk]` - for _visual_ relative line jumping. Type `10gj` for example, and you will jump
      down 10 lines _visually_ from the current position. This is useful when you have soft-wrap
      enabled.
- Respects your selection - if you start selecting text (for example, hit `v` while in IdeaVIM
  `NORMAL` mode) and then navigate using the command, the cursor will navigate to that position and
  your selection will be updated.

<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "
  goto"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it
  by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download
  the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains
  Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from
  disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/macintacos/goto/releases/latest) and
  install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from
  disk...</kbd>

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template

[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
