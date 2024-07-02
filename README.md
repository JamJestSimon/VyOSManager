#VyOS Manager App
This application came to be as a part of my thesis on "Mobile Application for configuration of selected features in VyOS Routers".
The application is eaesily expandable, as it's capabilities are based on provided configuration paths in corresponding JSON files.
##Current features
Currently it allows the user to configure the following:
- Ethernet and Loopback interfaces
- OSPF, RIP and Static routing protocols
- Firewall
- NAT
##Before using
Please note that before using the app, you'll need to do these steps:
1. Configure the HTTP API on your VyOS Router. Please make note of the password you've set, as you'll need it to connect to the HTTP API.
2. Generate a CA certificate and it's private key.
3. Generate a certificate that is signed by the CA.
4. Add the CA certificate and the signed certificate to the router.
5. Set the signed certificate to be used for connections with HTTP API
6. Add the CA certificate to the mobile device you'll be using. Remember to trust that certificate, as otherwise the untrusted connection will not work.
For a detailed description of these steps visit the following pages:
- https://docs.vyos.io/en/latest/configuration/service/https.html
- https://arminreiter.com/2022/01/create-your-own-certificate-authority-ca-using-openssl/
##Logging into the HTTP API
The first panel you'll see in the application is the connection panel.
Here you need to provide the app with the address of your VyOS router, and the password to it's HTTP API.
It is possible to save connection details for future use.
![Login Screen](https://github.com/JamJestSimon/VyOSManager/assets/50449327/01dc1372-d455-4f47-a5d7-8622dd36f377)
##Interface description
The application provides the user with a selection of categoriers, which can be changed by adding a category name to a list linked to the Navigation Drawer.
Each category displays the corresponding configuration currently set on the router. In case of submiting changes, the application will prompt the user to reload the displayed configuration.
![Configuration display](https://github.com/JamJestSimon/VyOSManager/assets/50449327/681d18a0-6cd8-4b31-8996-54d99f465df0)
It is possible to edit and delete configuration nodes, by using the "edit" and "delete" buttons next to the config node. The fields are scrollable to fit the necessaty content. Please note that not all fields are configurable for the sake of avoiding errors.
The configuratiopn tree is generated recursively from the JSON structure provided by the HTTP API. The application sends a request for configuraton with a path containing the category name, so if you need to monitor a different category, it just needs to be added to the CategoryName list in the MainScreen file.
The plus button on the bottom of the screen navigates the user to the adding configuration screen.
##Adding configuration
The application allows the user to add configurations to the connected VyOS router.
To add a configuration, select the category of your choosing and press the "plus" button at the bottom of the screen.
![Adding configuration form](https://github.com/JamJestSimon/VyOSManager/assets/50449327/abe85711-2429-41f8-a97f-9e284fb638e9)
While adding configuration, select your desired configuration path and fill in the required values and key names.
After reaching the end of a path, press the checkmark below to add the path to the list.
When you add all desired paths, press the plus button at the top of the screen. All added paths will be sent in one request to the router to avoid connectivity issues.
##Config path JSON structure
The application takes the avaliable config paths from a set of JSON files saved in the ConfigFormJsons folder.
To add a configuration category, create a JSON file with the category as it's name.
![Example JSON structure](https://github.com/JamJestSimon/VyOSManager/assets/50449327/05ae1ac8-66cd-4b31-a4ec-39df2411ed5d)
The structure of the JSON file is as following:
- Each field name needs to correspond to a possible configuration path.
- Field names that need to be provided by the user are placed in angle brackets.
- The field names on the deepest relative level need to be the second last configuration path entry, as the last one will be the "value" field of the form.
- The structure does not support objects in lists, so elements in lists are treated as field names on the deepest relative level.
