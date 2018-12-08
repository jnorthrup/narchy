https://wiki.debian.org/DebianInstaller/Modify/CD#Files_and_hooks
https://wiki.debian.org/Simple-CDD/Howto


a) debian installs a lot of unneceessary services, ex: cups*, sane*, exim*, etc
b) immediately change the fstab options for my hard drive to noatime
c) xanmod kernel as default
d) jdk12+ installed from the latest available .tar.gz - no apt/deb seems to exist yet but it just as easy to un tar the binaries somewhere (and also not install the official jre/jdk 11 or older)
e) non-free apt sources. should just make sure this is included by default with appropriate warning
