# ZipDoc
A Git `textconv` program to dump a ZIP files contents as text to stdout.

## Install
Store ZipDoc.class somewhere in your home directory, for example `~/bin` (use *this* path when "java **-cp ~/bin** ZipDoc" is used).

Define the diff filter in `~/.gitconfig`:
```
git config --global --replace-all diff.zipdoc.textconv "java -cp ~/bin ZipDoc"
```

Assign diff attributes to paths in `.gitattributes` or in `<repro-dir>/.git/info/attributes` for specific repository only.
 (also assigning the [rezip filter](https://github.com/costerwi/rezip) for efficient storage):
```
# MS Office
*.docx  filter=zip diff=zipdoc
*.xlsx  filter=zip diff=zipdoc
*.pptx  filter=zip diff=zipdoc
# OpenOffice
*.odt   filter=zip diff=zipdoc
*.ods   filter=zip diff=zipdoc
*.odp   filter=zip diff=zipdoc
# Misc
*.mcdx  filter=zip diff=zipdoc
*.slx   filter=zip diff=zipdoc
*.epub	filter=zip diff=zipdoc
```

