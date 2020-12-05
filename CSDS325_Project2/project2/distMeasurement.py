import matplotlib
matplotlib.use('Agg')
import struct
import time
import socket
import matplotlib.pyplot as plt

MSG = 'measurement for class project. questions to student akp96@case.edu or professor mxr136@case.edu'
PAYLOAD = bytes(MSG + 'a'*(1472 - len(MSG)), 'ascii')

MAX_HOPS = 100
PORT = 33434
MAX_TIME = 5


def measure(website, port):

    # obtain host
    try:
        ip_final = socket.gethostbyname(website)
    except socket.error:
        print("Unable to get host " + str(website))

    init_time = time.time()

    # creating sockets
    udp_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
    udp_sock.setsockopt(socket.SOL_IP, socket.IP_TTL, MAX_HOPS)
    udp_sock.sendto(PAYLOAD, (ip_final, port))
    udp_sock.close()

    icmp_sock = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_ICMP)
    icmp_sock.settimeout(MAX_TIME)

    # website connection
    try:
        data = icmp_sock.recvfrom(1500)[0]
        final_time = time.time()
        time_diff = final_time - init_time
        icmp_sock.close()

    # unsuccessful connection
    except socket.timeout:
        print("Unable to connect to ", website)
        icmp_sock.close()
        return None
    rem_ttl = data[36]
    hops = MAX_HOPS - rem_ttl

    # parse received packet
    packed_ip = data[28:48]
    ip_header = struct.unpack('!BBHHHBBH4s4s', packed_ip)
    ip_rec = socket.inet_ntoa(ip_header[9])
    rec_port = struct.unpack('!H', data[50:52])[0]

    # set vars to not modified if values are equal, otherwise set to modified
    port_match = "not modified" if rec_port == PORT else "modified"
    ip_match = "not modified" if ip_rec == ip_final else "modified"

    # check length of data
    if len(data) <= 56:
        rem_data = 0
    else:
        rem_data = len(data) - 56
    # print information
    print("Website name: ", website)
    print("Ip address: ", ip_final)
    print("If ports are modified: ", port_match)
    print("If ip addresses are modified: ", ip_match)
    print("Data remaining: ", rem_data)
    print("Time difference: ", time_diff)
    print("Hops: ", hops)
    print("\r\n")
    return hops, time_diff


def main():
    file = open("targets.txt", "r")
    print(file.read())

    # read text file with websites
    website_list = open("targets.txt").read().splitlines()
    # empty lists
    list1 = []
    list2 = []
    # creating scatter plot
    for website in website_list:
        temp_hops, temp_time_diff = measure(website, PORT)
        list1.append(temp_hops)
        list2.append(temp_time_diff)
        plt.scatter(temp_hops, temp_time_diff, label=website, s=10.0)

    # set x labels, y labels, legend
    plt.xlabel("Number of hops")
    plt.ylabel("RTT (s)")
    plt.legend(fontsize='x-small')
    plt.savefig("correlation.png")


if __name__ == "__main__":
    main()
