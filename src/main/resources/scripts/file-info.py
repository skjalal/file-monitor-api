from datetime import datetime
import os
import pwd
import subprocess
import json
import sys

path = sys.argv[1]
data = {}

# file modification timestamp of a file
m_time = os.path.getmtime(path)
# convert timestamp into DateTime object
dt_m = datetime.fromtimestamp(m_time).strftime("%Y-%m-%d %H:%M:%S")
data['lastModifiedDate'] = dt_m

# file creation timestamp in float
c_time = os.path.getctime(path)
# convert creation timestamp into DateTime object
dt_c = datetime.fromtimestamp(c_time).strftime("%Y-%m-%d %H:%M:%S")
data['createdDate'] = dt_c

# file last access timestamp in float
a_time = os.path.getatime(path)
# convert last access timestamp into DateTime object
dt_a = datetime.fromtimestamp(a_time).strftime("%Y-%m-%d %H:%M:%S")
data['lastAccessedDate'] = dt_a

data['fileSize'] = os.path.getsize(path)

file_stats = os.stat(path)

data['createdBy'] = pwd.getpwuid(file_stats.st_uid).pw_name

fileName = path[path.rindex('/')+1:]

cmd = "sudo ausearch -f " + fileName +" -i"
p = subprocess.Popen(cmd, stdout=subprocess.PIPE, shell=True)
(output, err) = p.communicate()
p_status = p.wait()
out1 = output[output.rindex("type=SYSCALL"):]
result = out1[out1.index(" uid="):out1.index("gid")].replace("uid=", "")
data['lastModifiedBy'] = result.strip()
print json.dumps(data)
print ("Command exit status/return code : ", p_status)