package pl.lapko.vyosmanager.data

data class VyOSDevices(
    var devices : MutableMap<String, VyOSPassword>
)

data class VyOSPassword(
    var password : String
)