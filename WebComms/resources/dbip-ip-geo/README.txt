
========================
dbip-city-2016-10.csv.gz
========================

This file holds the IP->Geo lookup information used by WebComms.

This functionality was implemented under {artf175370}.

To use, unzip this file somewhere in your workspace or elsewhere on your computer.  

It will expand to a ~500 MB CSV file, which you should never check in.

Then add the following system property to your "Avondale Server" run configuration:

  -Dwebcomms.dbip.file=<FILE>

where FILE is the path to the expanded CSV file.

The Avondale Analytics Job State UI can then be provided valid Geo info from WebComms.

The file we have dates back to October 2016 so we may want to make an effort to update it eventually.
