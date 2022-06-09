jq '. | {schema: tojson}' "$1" | \
  curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
          -d @- "$2"/subjects/"$3"/versions
