#!/bin/bash
# This script generates xml definition of left menu for fresco project.
#
# output structure of file is as follows
# <root> 
#    <folder name="folderName">
#	<a href="path-to-file"filename.xml">
#    </folder>
# </root>
#
# many folders can be presented as many files in one folder

# output filename
MENU=main-menu.xml

# remove current
rm $MENU

# create empty
touch $MENU

# write header
echo '<?xml version="1.0" encoding="UTF-8"?>' >> $MENU
# load formatter
echo '<?xml-stylesheet version="2.0" type="text/xsl" href="MenuFormatter.xsl"?>' >> $MENU
# create root and title for file
echo "<root><title>List of workers</title>" >> $MENU

# root folder for searching xml files
FOLDER=`pwd`

# in $folder read each subfolder (one after another) and list its content
find $FOLDER|grep ".xml$"|sed -e 's/.*workers/workers/g'|sed -e 's/\/[a-zA-Z0-9]*\.xml$//g'|while read folder 
do echo "<folder name=\"$folder\">" >> $MENU
# convert filenames and names of workers into specified form
find .|grep "./$folder/[a-zA-Z0-9]*.xml$"|xargs grep "worker name=" -H|sed -e's/^\.\//<a\ href=\"/g'|sed 's/\:\t*/\"\>/g'|sed 's/<worker name=\"//g'|sed 's/">$/<\/a\>/g' >> $MENU
# close folder
echo "</folder>" >> $MENU
done
#close file
echo "</root>" >> $MENU
