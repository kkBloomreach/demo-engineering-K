# Following code to set up Python's package path
import sys
import os

cwd=os.getcwd()
packages=['utils','datahub', 'dataconnect']
pkgpath=cwd
for pkg in packages:
    pkgpath = "%s:%s/%s" % (pkgpath, cwd, pkg)
sys.path.append (pkgpath)

