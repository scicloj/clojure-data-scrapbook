#! /bin/bash

rm -rf copies_of_projects

cat base.yml > _quarto.yml

files=$(find ../projects/ -name '*.qmd' | sed -e 's/^..\///' | sort)
echo $files

for file in $files
do
    target="copies_of_"$file
    source="../"$file
    mkdir -p "${target%/*}"
    cp $source $target
    echo "    - "$target >> _quarto.yml
done

cat _quarto.yml

quarto preview
