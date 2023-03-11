import datetime
import os
import pwd

# Path to the file
path = r"/var/local/test.txt"

# file modification timestamp of a file
m_time = os.path.getmtime(path)
# convert timestamp into DateTime object
dt_m = datetime.datetime.fromtimestamp(m_time)
print('Modified on:', dt_m)

# file creation timestamp in float
c_time = os.path.getctime(path)
# convert creation timestamp into DateTime object
dt_c = datetime.datetime.fromtimestamp(c_time)
print('Created on:', dt_c)

# file last access timestamp in float
a_time = os.path.getatime(path)
# convert last access timestamp into DateTime object
dt_a = datetime.datetime.fromtimestamp(a_time)
print('Accessed on:', dt_a)

print('File size: ', os.path.getsize(path))

file_stats = os.stat(path)

print('Owner: ', pwd.getpwuid(file_stats.st_uid).pw_name)

os.system('sudo ausearch -k test-file -i')