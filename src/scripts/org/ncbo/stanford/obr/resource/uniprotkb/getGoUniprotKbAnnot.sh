#!/bin/sh

annotationFile="gene_association.goa_human"

# create a directory for annotations
mkdir ./files/resources/go_annotations
mkdir ./files/resources/go_annotations/homo_sapiens

# get annotation file by FTP
ftp -inv ftp.geneontology.org<<EOF 
user anonymous anonymous
cd ./pub/go/gene-associations
lcd ./files/resources/go_annotations/homo_sapiens
binary
mget gene_association.goa_human.gz
bye
EOF
# gunzip the downloaded file
cd ./files/resources/go_annotations/homo_sapiens
gunzip -f $annotationFile".gz"

# extract gene symbole and associated GO ID 
cat $annotationFile | awk 'BEGIN{FS="\t"}{ 
  if($1- eq "UniProtKB"){
    localElementID = $2
    gene_symbole   = $3
    if($4 ~ /^GO.*$/) {
      go_annotation = $4
      go_branch = $8
      pn_name = $9
    }else if($5 ~ /^GO.*$/) {
      go_annotation = $5
      go_branch = $9
      pn_name = $10
    }
 
  print(localElementID,"\t",gene_symbole,"\t",pn_name,"\t",go_branch,"\t",go_annotation)
  }
}'
