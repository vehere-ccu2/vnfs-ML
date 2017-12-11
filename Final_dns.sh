now="$(date +'%d-%m-%Y')"
echo capture-$now
lesstime="$(date +'%d-%m-%Y %H:%M:%S')"
echo $lesstime
newtime="$(date +%s%3N)"
echo $newtime
ptime= date --date '-30 min' 
echo $ptime
p1time="$(date --date '-30 min' +%s%3N)"
echo $p1time


curl -XGET "192.168.2.244:9200/capture-$now/metadata/_search?size=10000" -H 'Content-Type: application/json' -d '{"_source": ["_id","_index","layers.frame.frame_frame_time", "layers.frame.frame_frame_len", "layers.ip.ip_ip_src", "layers.ip.ip_ip_dst", "layers.dns.text_dns_qry_name", "layers.dns.text_dns_resp_type", "layers.dns.text_dns_qry_class", "layers.dns.text_dns_qry_name_len", "layers.dns.text_dns_qry_type", "layers.dns.text_dns_a","layers.dns.dns_flags_dns_flags_rcode","layers.frame.frame_frame_time_epoch"], "query": {"constant_score": {"filter": {"bool":{"must":[{"range" :{"timestamp" : {"gt":'$p1time', "lte": '$newtime', "format": "epoch_millis"}}},{"exists":{"field":"layers.dns.text_dns_qry_name"}}]}}}}}' | jq -cM '.hits.hits[] | ._source.layers.ip + ._source.layers.dns + ._source.layers.frame + {"id": ._id} + {"index": ._index}' | jq -cM 'to_entries |  
	map(if .key == "id" 
	then . + {"key":"id"}
	else .
	end
	) |
	map(if .key == "text_dns_qry_name" 
	then . + {"key":"dns_qry_name"}
	else .
	end
	) |
	map(if .key == "text_dns_resp_type" 
	then . + {"key":"dns_resp_type"}
	else .
	end 
	) | 
	map(if .key == "text_dns_qry_class" 
	then . + {"key":"dns_qry_class"}
	else .
	end 
	) |
	map(if .key == "text_dns_qry_name_len" 
	then . + {"key":"dns_qry_name_len"} 
	else .
	end 
	) |
        map(if .key == "text_dns_qry_type" 
	then . + {"key":"dns_qry_type"} 
	else .
	end 
	) |
        map(if .key == "text_dns_a" 
	then . + {"key":"dns_a"} 
	else .
	end 
	) |
        map(if .key == "frame_frame_time" 
	then . + {"key":"frame_time"} 
	else .
	end 
	) |
        map(if .key == "frame_frame_len" 
	then . + {"key":"frame_len"} 
	else .
	end 
	) |
        map(if .key == "ip_ip_dst" 
	then . + {"key":"ip_src"} 
	else .
	end 
	) |
        map(if .key == "ip_ip_src" 
	then . + {"key":"ip_dst"} 
	else .
	end 
	) |
        map(if .key == "frame_frame_time_epoch" 
	then . + {"key":"unix_tstamp"} 
	else .
	end 
	) |
       map(if .key == "dns_flags_dns_flags_rcode" 
	then . + {"key":"dns_qry_rcode"} 
	else .
	end 
	) |
from_entries '  | jq -c . > "/home/users/spark-2.0.2-bin-hadoop2.7/bin/DNS_DATA/Dnsdata.json"

 /home/users/spark-2.0.2-bin-hadoop2.7/bin/spark-submit --class "org.apache.spot.SuspiciousConnects" --master local /home/users/spark-2.0.2-bin-hadoop2.7/bin/target/scala-2.10/spotmldns.jar --analysis "dns"  --input "/home/users/spark-2.0.2-bin-hadoop2.7/bin/DNS_DATA/Dnsdata.json"   --dupfactor 1000   --feedback "/home/users/spark-2.0.2-bin-hadoop2.7/bin/feedback.csv"   --ldatopiccount 20 --scored /home/users/spark-2.0.2-bin-hadoop2.7/bin/scores   --threshold 1 --maxresults -1 --esnode "192.168.2.244" --esport "9200"





















