#!/usr/bin/env bash

rm nquad nquad.*

echo 'Downloading Schema.org...'
wget "http://schema.org/version/latest/all-layers.nq" -O nquad

echo 'Downloading NASA Sweet...'
rm -Rf sweet
#wget https://sweet.jpl.nasa.gov/sites/default/files/2.3.zip
git clone --depth 1 https://github.com/ESIPFed/sweet
cd sweet
for f in *.owl; do
rapper $f -o nquads > $f.nquad
done
cd ..
cat sweet/*.nquad >> nquad


echo 'Downloading other OWL ontologies...'
SRC=(
https://www.w3.org/1999/02/22-rdf-syntax-ns
https://www.w3.org/2000/01/rdf-schema
http://xmlns.com/foaf/spec/index.rdf
http://www.w3.org/2002/07/owl
#http://www.adampease.org/OP/WordNet.owl
#http://www.adampease.org/OP/SUMO.owl
https://files.ifi.uzh.ch/ddis/ontologies/evoont/2008/11/som/
https://files.ifi.uzh.ch/ddis/ontologies/evoont/2008/11/bom/
https://files.ifi.uzh.ch/ddis/ontologies/evoont/2008/11/vom/
https://raw.githubusercontent.com/knowrob/knowrob/master/knowrob_common/owl/knowrob.owl
)
for i in ${SRC[*]}
do
echo
echo "  Downloading $i .."
wget "$i" -O x.owl --unlink
echo "    Converting and appending to 'nquad'"
rapper x.owl -o nquads >> nquad
rm x.owl
echo
done


echo 'Compressing nquad ...'
gzip -k -9 nquad

wc nquad
ls -l nquad*
