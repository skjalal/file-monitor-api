import subprocess
cmd = "sudo ausearch -k test-file -i"
p = subprocess.Popen(cmd, stdout=subprocess.PIPE, shell=True)
(output, err) = p.communicate()
p_status = p.wait()
print "Command exit status/return code : ", p_status