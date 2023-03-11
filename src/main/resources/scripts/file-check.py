import subprocess
cmd = "% s % s %s" % ("sudo ausearch -f", a, "-i")
print "Executing shell: ", cmd
p = subprocess.Popen(cmd, stdout=subprocess.PIPE, shell=True)
(output, err) = p.communicate()
p_status = p.wait()
print "Command exit status/return code : ", p_status