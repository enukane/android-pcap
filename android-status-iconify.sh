#!/bin/sh

convert $1 -geometry 18x18 res/drawable-ldpi/ic_statusbar.png
convert $1 -geometry 24x24 res/drawable-mdpi/ic_statusbar.png
convert $1 -geometry 36x36 res/drawable-hdpi/ic_statusbar.png
convert $1 -geometry 48x48 res/drawable-xhdpi/ic_statusbar.png

