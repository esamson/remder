# Remder

Renders a live HTML view of a Markdown file. Bring your own editor.

![demo](https://esamson.github.io/remder/static/remder-demo.gif)

## Install

*Tested on [Oracle Java 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html).*

Use [Coursier](https://github.com/coursier/coursier#generating-bootstrap-launchers)
to add the **remder-app** launcher to your `$PATH`. For example (assuming
`~/.local/bin` is in your `$PATH`):

```
$ coursier bootstrap ph.samson.remder:remder-app_2.12:0.0.7 -o ~/.local/bin/remder-app
```

Check out [Maven Central](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22ph.samson.remder%22%20AND%20a%3A%22remder-app_2.12%22)
for the latest available releases and snapshots.

**note:** If you don't have **coursier**, see [here](https://github.com/coursier/coursier#command-line).

## Use

Provide the markdown file as an argument to **remder-app**.

```
$ remder-app README.md
```

This launches the preview window. Edit with your favorite editor and **remder**
automatically picks up and renders any changes when you save your edits.

### Use with Vim

I use [vim-markdown](https://github.com/plasticboy/vim-markdown) for my own
markdown editing needs. In my `~/.vimrc` I map `\r` to launch **remder-app**.

```
autocmd Filetype markdown nmap <leader>r :w \| :silent !remder-app '%' &<CR>
```

## Embedded Diagrams with PlantUML

You can embed [PlantUML](http://plantuml.com/) diagrams as a `uml` code block.

    ```uml
    Alice -> Bob: Authentication Request
    Bob --> Alice: Authentication Response

    Alice -> Bob: Another authentication Request
    Alice <-- Bob: another authentication Response
    ```

Sequence diagrams work out of the box but for other diagram types you will
need to have Graphviz (the `dot` command) on your system.
See [PlantUML Installation notes](http://plantuml.com/faq-install).

## View in Browser

To view a (static) rendering in your default browser, press **`b`** on your
keyboard. There you'll have all the nice things your browser can do for you
like printing your document and copy-pasting the rendered HTML into Gmail or
Google Docs. (Actually, in my testing, copy-pasting rendered HTML from the
**remder** view works on *Mac OS X* but not in *Fedora*).
