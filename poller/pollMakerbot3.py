import makerbotapi
import sys
import os
import ctypes
import struct
import io
import png
import time
import requests
import operator
import socket
import logging
from datetime import datetime

temperature = '-1'
process = 'Unknown'
step = 'Unknown'
filament_extruded = '-1'
time_remaining = '-1'
progress = '-1'
error = 'No error.'
start_time = 'Unknown'

def removeQuotes(str):
	return str[:str.rfind('"')] #[str.index('"'):str.rfind('"')]

makerbot = ''
def extract(response, token):	
	#response = self.rpc_socket.recv(2048)	
	value = 'Unknown'

	if token in response:
		a = response.index(token)
		tresponse = response[a+len(token):]
		b=0
		try:
			b = tresponse.index(',')
			c = tresponse.index('}')
			if ( c< b):
				b=c
		except:
                        logging.exception('')
			return 'Unknown' #temperature #return extract_temperature(self,response, token):
		value = tresponse[:b]
	return value


def get_values():
	global temperature 
	global process 
	global step 
	global filament_extruded 
	global time_remaining
	global progress 
	global error 
	global start_time 
	bytes_to_read = 2048

	extrusion_mass_g = elapsed_time = 0
	step = "Idle"
	
	makerbot.rpc_socket.setblocking(0)
	try:
		response = makerbot.rpc_socket.recv(bytes_to_read)
		while( len(response) ==  bytes_to_read): #skip to the end of the stream
			response = makerbot.rpc_socket.recv(bytes_to_read)
	except socket.error:
		#no data available, use blocking socket to wait
                logging.exception('')
		pass
	makerbot.rpc_socket.setblocking(1)
	
	response += makerbot.rpc_socket.recv(bytes_to_read)
	
	token = '"current_temperature":'
	temperature = str(extract(response, token))
	if (temperature == 'Unknown'): #try again
                temperature = str(-1)
		return None
	print "current_temperature "+temperature

	token = '"current_process":'
	process = extract(response, token)
	if (process == 'Unknown'):
		process = 'Unable to determine process'
		return None
	else:
		token = '"name": "'
		if token in response:
			a = response.index(token)
			tresponse = response[a+len(token):]
			try:
				b = tresponse.index('"')
			except:
				process = 'Unable to determine process'
				return None
			process = tresponse[:b]

			token = '"step": '
			step = extract(tresponse, token)
			step = step.replace('\"','')
			print "step "+step

			token = '"filament_extruded": '
			filament_extruded = str(extract(response, token))
			if ( filament_extruded == 'Unknown'):
                                filament_extruded = str(-1)
			print "filament_extruded "+filament_extruded
			
			token = '"time_remaining": '
			time_remaining = str(extract(response, token))
			if ( time_remaining == 'Unknown'):
                                time_remaining = str(-1)
			print "time_remaining "+time_remaining

			token = '"progress": '
			progress = str(extract(response, token))
			if ( progress == 'Unknown'):
                                progress = str(-1)
			print "progress "+progress
			
			token = '"error": '
			errorToken = extract(response, token)
			error = "No error."
			if (errorToken.strip() != "null"):
				token = '"code": '
				code = extract(response, token)
				if ( code=="80"):
					error = "Out of filament."                        
				elif ( code=="81"):
					error = "Filament is jammed."                        
										
			print "error "+error

			token = '"start_time": '
			start_time = extract(response, token)			
			start_time = start_time.replace('\"','')
			print "start_time "+start_time
				
		else:
			process = 'Idle'
			filament_extruded = '-1'
			time_remaining = '-1'
			progress = '-1'
			error = 'No error.'
			start_time = 'Unknown'
			step = 'Unknown'
									
		print "process " + process
	
	return None #,extrusion_mass_g,elapsed_time,step 

if __name__ == '__main__':

	
	if len(sys.argv) < 2:
		print "args: <ip address of your gen5> [<AUTH CODE>]"
		sys.exit(1)
	ip_address = sys.argv[1]

	if len(sys.argv) > 2:
		AUTH_CODE = sys.argv[2]
	else:
		AUTH_CODE = None

	makerbot = makerbotapi.Makerbot(ip_address, auth_code=AUTH_CODE)
	if AUTH_CODE == None:
		print "Press the flashing action button on your makerbot now"
		makerbot.authenticate_fcgi()
		print "Authenticated with code ", makerbot.auth_code

	makerbot.authenticate_json_rpc()

	name = 'temp.txt'

	#get initial values
	botstate = makerbot.get_system_information()
	temperature = str(botstate.toolheads[0].current_temperature)
	if (botstate.current_process):
		process = botstate.current_process.name
	else:
		process = "Idle"
		
	try:
		print temperature
		print process
		tmpFile = open(name,'w')
		tmpFile.write(temperature)
		tmpFile.write("\n%s" % process)
		tmpFile.write("\n%s" % filament_extruded)
		tmpFile.write("\n%s" % time_remaining)
		tmpFile.write("\n%s" % progress)
		tmpFile.write("\n%s" % error)
		tmpFile.write("\n%s" % start_time)
		tmpFile.write("\n%s" % step)
		tmpFile.close()
	except:
		print 'file error'
		logging.exception('')
	
	while(1):
		#_, width, height, _, yuv_image = makerbot._get_raw_camera_image_data()
		#rgb_rows = makerbot._yuv_to_rgb_rows(StringIO(yuv_image), width, height)
                try:
                        makerbot.save_camera_png('C:/1worxdemo/makerbot/edgeserver/camera.jpg')
                except:
                        print 'Error reading camera'
			tmpFile = open('error.log','w')
			tmpFile.write("Error reading camera at %s" % str(datetime.now()))
			tmpFile.close()

		get_values()
		#,extrusion_mass_g,elapsed_time,step
		try:
			#payload = {'Temperature':str(toolhead.current_temperature)}
			#response = requests.put("http://localhost:8000/Thingworx/MakerBotEMS/Properties/Temperature",data=payload)
			#print response
			tmpFile = open(name,'w')
			tmpFile.write(str(temperature))
			tmpFile.write("\n%s" % process)
			tmpFile.write("\n%s" % filament_extruded)
			tmpFile.write("\n%s" % time_remaining)
			tmpFile.write("\n%s" % progress)
			tmpFile.write("\n%s" % error)
			tmpFile.write("\n%s" % start_time)
			tmpFile.write("\n%s" % step)
			tmpFile.close()
		except:
			print 'Write to file error'
			logging.exception('')

		time.sleep(1)
		

