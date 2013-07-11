#!/bin/bash


# get current timestamp
current_date=$(date +%s)

# calculate one month
month=$((60 * 60 * 24 * 31))

# go one month back
last_month=$(($current_date - $month))

# convert to java timestamp (add miliseconds)
last_month="${last_month}000"

# remove logs
echo "delete from logs where date<'${last_month}';" | psql -d db_keysrv 
