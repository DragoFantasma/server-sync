#!/usr/bin/python
import socket
from threading import Thread
import os
import sys

if len(sys.argv) < 1:
    print("Please type the server folder")
    sys.exit(1)

port = 3750

mods = os.listdir(sys.argv[1])

print(f"Loaded mods list:")

for mod in mods:
    print(f"- {mod}")

def address_to_string(a):
    return f"{a[0]}:{a[1]}"

def send_mod_to_client(client, mod_name):
    conn, addr = client

    print(f"Syncing {mod_name}...")
    
    # Telling the client the mod name length and content
    conn.sendall((len(mod_name)).to_bytes(1, 'little'))
    conn.sendall(mod_name.encode())


    with open(sys.argv[1] + "/" + mod_name, 'rb') as f:
        content = f.read()

    print(f"Sending {len(content)} bytes to {address_to_string(addr)}")

    # Telling the client the mod content length
    conn.sendall(len(content).to_bytes(4, 'little'))

    # Sending the content
    conn.sendall(content)


def handle_connection(conn, addr):
    try:
        with conn:
            print(f"Connected from {addr}")

            welcome_message = conn.recv(1024).decode('utf-8')

            print(welcome_message)

            if welcome_message != "FOLDER_OK":
                print(f"The client encounter some error, connection closed")
                return


            # Strip out the newline
            mods_list = conn.recv(2048).decode('utf-8')[:-1]

            print(f"Mods list from {address_to_string(addr)}:")

            for mod in mods_list.split(","):
                print(mod)

            # Sync client's mods to server's mods list
            for mod in mods:
                if mod not in mods_list:
                    send_mod_to_client((conn, addr), mod)
                else:
                    print(f"Mod {mod} is already up to date")
    
        conn.close()
        print(f"Connection with {address_to_string(addr)} was closed")

    except Error as e:
        print(f"Error while syncing mod to {address_to_string(addr)}.\nStack trace: {e}")

def wait_connection(s):
    try:
        conn, addr = s.accept()
    except KeyboardInterrupt:
        print("** Stopped **")
        sys.exit(1)

    t = Thread(target=handle_connection, args=(conn, addr,))
    t.start()


with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    s.bind(('localhost', port))
    s.listen(1)

    print(f"Started server on localhost:{port}")

    while True:
        wait_connection(s)