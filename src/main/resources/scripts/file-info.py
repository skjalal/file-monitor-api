from datetime import datetime
import os
import pwd
import subprocess

# Path to the file
path = r"/var/local/test.txt"

# file modification timestamp of a file
m_time = os.path.getmtime(path)
# convert timestamp into DateTime object
dt_m = datetime.fromtimestamp(m_time)
print('Modified on:', dt_m)

# file creation timestamp in float
c_time = os.path.getctime(path)
# convert creation timestamp into DateTime object
dt_c = datetime.fromtimestamp(c_time)
print('Created on:', dt_c)

# file last access timestamp in float
a_time = os.path.getatime(path)
# convert last access timestamp into DateTime object
dt_a = datetime.fromtimestamp(a_time)
print('Accessed on:', dt_a)

print('File size: ', os.path.getsize(path))

file_stats = os.stat(path)

print('Owner: ', pwd.getpwuid(file_stats.st_uid).pw_name)

cmd = "sudo ausearch -k test-file -i"
p = subprocess.Popen(cmd, stdout=subprocess.PIPE, shell=True)
(output, err) = p.communicate()
p_status = p.wait()
out1 = output[output.rindex("type=SYSCALL"):]
result = out1[out1.index(" uid="):out1.index("gid")].replace("uid=", "")
print result.strip()
print "Command exit status/return code : ", p_status