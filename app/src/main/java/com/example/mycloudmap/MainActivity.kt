package com.example.mycloudmap

// import androidx.activity.result.ActivityResultLauncher
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.UiSettings
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.maps.DirectionsApi
import com.google.maps.DirectionsApiRequest
import com.google.maps.GeoApiContext
import com.google.maps.android.SphericalUtil
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TravelMode
import com.google.maps.model.Unit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener,
    GoogleMap.OnMapLongClickListener {

    private lateinit var txtLatitud: TextView
    private lateinit var txtLongitud: TextView
    private lateinit var txtOrigen: TextView
    private lateinit var txtDestino:TextView
    private lateinit var mMap: GoogleMap
    private lateinit var btnGuardarUbicacion: Button
    private lateinit var btnGuardarRecorrido: Button
    private lateinit var btnCerrarSesion: Button
    private lateinit var btnOrigen:Button
    private lateinit var btnDestino:Button
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var databaseReference: DatabaseReference
    private lateinit var mTextViewName: TextView
    private lateinit var mTextViewEmail: TextView
    private var origenMarker: Marker? = null
    private var destinoMarker: Marker? = null
    private var origenLocation: LatLng? = null
    private var destinoLocation: LatLng? = null

    // private lateinit var fromPlaceLauncher: ActivityResultLauncher<Intent>
    private val fromPlaceLauncherForOrigen = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            if (data != null) {
                val lugar = Autocomplete.getPlaceFromIntent(data)
                Log.i(TAG, "Ubicacion: ${lugar.name}, ${lugar.id}")
                Log.i(TAG, "Ubicacion: ${lugar.latLng}")
                Log.i(TAG, "Ubicacion: ${lugar.address}")
                handleOrigenSelection(lugar)
            }
        } else if (result.resultCode == AutocompleteActivity.RESULT_ERROR) {
            val data: Intent? = result.data
            val status = Autocomplete.getStatusFromIntent(data!!)
            status.statusMessage.also {
                showToast(status.statusMessage)
            }
        } else if (result.resultCode == RESULT_CANCELED) {
            showToast("La selección fue cancelada")
        }
    }

    private val fromPlaceLauncherForDestino = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            if (data != null) {
                val lugar = Autocomplete.getPlaceFromIntent(data)
                Log.i(TAG, "Ubicacion: ${lugar.name}, ${lugar.id}")
                Log.i(TAG, "Ubicacion: ${lugar.latLng}")
                Log.i(TAG, "Ubicacion: ${lugar.address}")
                handleDestinoSelection(lugar)
            }
        } else if (result.resultCode == AutocompleteActivity.RESULT_ERROR) {
            val data: Intent? = result.data
            val status = Autocomplete.getStatusFromIntent(data!!)
            status.statusMessage.also {
                showToast(status.statusMessage)
            }
        } else if (result.resultCode == RESULT_CANCELED) {
            showToast("La selección fue cancelada")
        }
    }
    private fun agregarMarcadores(latitud: Double, longitud: Double, esOrigen: Boolean) {
        val latLng = LatLng(latitud, longitud)
        val markerOptions = MarkerOptions().position(latLng)

        val titulo = if (esOrigen) "Origen" else "Destino"
        markerOptions.title(titulo)

        if (esOrigen) {
            origenMarker?.remove()
            origenMarker = mMap.addMarker(markerOptions)
        } else {
            destinoMarker?.remove()
            destinoMarker = mMap.addMarker(markerOptions)
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

/*

    @SuppressLint("SetTextI18n")
    private fun ubicacionesSeleccionadas(lugar: Place) {
        val latLng = lugar.latLng
        if (latLng != null) {
            val latitud = latLng.latitude
            val longitud = latLng.longitude
            agregarMarcadores(latitud, longitud, lugar== txtOrigen)
            if (origenLocation != null && destinoLocation != null) {
                // Dibuja la ruta en el mapa usando origenLocation y destinoLocation.
                // Añade código para trazar la ruta aquí.
                val origenLatLng = origenLocation?.let { LatLng(it.latitude, it.longitude) }
                val destinoLatLng = destinoLocation?.let { LatLng(it.latitude, it.longitude) }
                if (origenLatLng != null) {
                    if (destinoLatLng != null) {
                        dibujarRuta(origenLatLng, destinoLatLng)
                    }
                    val distanciaEnMetros = destinoLatLng?.let {
                        calcularDistancia(origenLatLng,
                            it
                        )

                        val distanciaEnKilometros = distanciaEnMetros / 1000.0
                    }
                    val txtDistancia = findViewById<TextView>(R.id.txtDistancia)
                    txtDistancia.text = "Distancia: $distanciaEnMetros metros"
                    showToast("Distancia: $distanciaEnKilometros metros")
                }
            }
        } else {
            Log.e(TAG, "No se pudo obtener la ubicación (latLng es nulo).")
        }
    }
*/

    @SuppressLint("SetTextI18n")
    private fun ubicacionesSeleccionadas(lugar: Place) {
        val latLng = lugar.latLng
        if (latLng != null) {
            val latitud = latLng.latitude
            val longitud = latLng.longitude
            agregarMarcadores(latitud, longitud, lugar == txtOrigen)

            // Verifica que tengas ubicaciones de origen y destino válidas
            if (origenLocation != null && destinoLocation != null) {
                val origenLatLng = LatLng(origenLocation!!.latitude, origenLocation!!.longitude)
                val destinoLatLng = LatLng(destinoLocation!!.latitude, destinoLocation!!.longitude)

                // Dibuja la ruta en el mapa
                dibujarRuta(origenLatLng, destinoLatLng)

                // Calcula la distancia en metros y kilómetros
                val distanciaEnMetros = calcularDistancia(origenLatLng, destinoLatLng)
                val distanciaEnKilometros = distanciaEnMetros / 1000.0

                val distanciaFormateada = String.format("%.2f", distanciaEnKilometros)

                // Supongamos que ya tienes el total de compra
                val totalDeCompra = 45000.0 // Reemplaza con el total de compra real

                // Inicializa el costo de despacho
                var costoDeDespacho = 0.0

                // Verifica si el total de compra califica para el servicio gratuito dentro del radio de 20 km
                if (totalDeCompra > 50000 && distanciaEnKilometros <= 20) {
                    // El servicio es gratuito
                    costoDeDespacho = 0.0
                } else {
                    // Calcula el costo de despacho según las reglas
                    val tarifaPorKilometro = when {
                        totalDeCompra >= 25000 && totalDeCompra <= 49999 -> 150.0
                        else -> 300.0
                    }

                    costoDeDespacho = tarifaPorKilometro * distanciaEnKilometros

                }

                // Muestra la distancia y el costo en un TextView
                val txtDistancia = findViewById<TextView>(R.id.txtDistancia)
                txtDistancia.text = "Distancia: $distanciaFormateada KM"
                // Muestra el costo
                val txtCostoDeDespacho = findViewById<TextView>(R.id.txtCostoDeDespacho)
                val costoFormateado = String.format("%.2f", costoDeDespacho)
                txtCostoDeDespacho.text = "Costo de Despacho: $costoFormateado pesos"


                // Muestra la distancia en kilómetros en un mensaje Toast
                showToast("Distancia: $distanciaEnKilometros KM")

              /*  // Mueve la cámara para enfocarse en la distancia entre los puntos de origen y destino
                val builder = LatLngBounds.Builder()
                builder.include(origenLatLng)
                builder.include(destinoLatLng)
                val bounds = builder.build()
                val padding = 200 // Ajusta este valor según tus preferencias
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))*/
            }
        } else {
            Log.e(TAG, "No se pudo obtener la ubicación (latLng es nulo).")
        }
    }

    private fun calcularDistancia(origen: LatLng, destino: LatLng): Double {
        // Calcula la distancia entre origen y destino
        return SphericalUtil.computeDistanceBetween(origen, destino)
    }

    private fun dibujarRuta(origen: LatLng, destino: LatLng) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val context = GeoApiContext.Builder()
                    .apiKey("AIzaSyCpIqh5iD3HKD8qQBwkQoSs3sjbjaUA-_A") // Reemplaza con tu propia clave de API de Google Maps
                    .build()

                // Define las opciones de la solicitud de direcciones
                val request: DirectionsApiRequest= DirectionsApi.newRequest(context)
                    .origin(com.google.maps.model.LatLng(origen.latitude, origen.longitude))
                    .destination(com.google.maps.model.LatLng(destino.latitude, destino.longitude))
                    .mode(TravelMode.DRIVING) // Modo de viaje (puede ser WALKING, BICYCLING, etc.)
                    .units(Unit.METRIC)


                // Realiza la solicitud para obtener la ruta
                val result: DirectionsResult = request.await()

                // Verifica si se encontró una ruta
                if (result.routes.isNotEmpty()) {
                    // Obtiene la polyline de la ruta
                    val polyline = result.routes[0].overviewPolyline.decodePath()

                    // Utiliza Dispatchers.Main para las operaciones de la interfaz de usuario
                    withContext(Dispatchers.Main) {

                        // Borra cualquier ruta anterior en el mapa
                        mMap.clear()

                        // Dibuja la nueva ruta en el mapa
                        val polyLineOptions = PolylineOptions()
                        for (point in polyline) {
                            polyLineOptions.add(LatLng(point.lat, point.lng))
                        }
                        mMap.addPolyline(polyLineOptions)

                        // Asegúrate de que los marcadores de origen y destino sigan siendo visibles
                        agregarMarcadores(origen.latitude, origen.longitude, true)
                        agregarMarcadores(destino.latitude, destino.longitude, false)

                        // Mueve la cámara para enfocarse en la distancia entre los puntos de origen y destino
                        val builder = LatLngBounds.Builder()
                        builder.include(origen)
                        builder.include(destino)
                        val bounds = builder.build()
                        val padding = 200 // Ajusta este valor según tus preferencias
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))

                        }
                    }
                } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }
            //Esta función utiliza la API de direcciones de Google Maps para obtener la ruta entre las ubicaciones de origen y destino y luego la dibuja en el mapa. Asegúrate de reemplazar "TU_API_KEY" con tu propia clave de API de Google Maps. También puedes personalizar el modo de viaje y otros parámetros según tus necesidades.

   // Llama a esta función después de configurar las ubicaciones de origen y destino en las variables origenLocation y destinoLocation en tu código. Por ejemplo:

   // Llama a la función para dibujar la ruta cuando tengas ubicaciones de origen y destino válidas.

    companion object {
        const val REQUEST_CODE_LOCATION = 0
       // private const val REQUEST_CODE_AUTOCOMPLETE_FROM = 1
        // private const val REQUEST_CODE_AUTOCOMPLETE_TO = 2
       const val TAG = "MainActivity"
    }


    @SuppressLint("MissingInflatedId", "SetTextI18n", "StringFormatInvalid")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.btnOrigen = findViewById<View>(R.id.btnOrigen) as Button
        this.btnDestino = findViewById<View>(R.id.btnDestino) as Button
        this.btnGuardarUbicacion = findViewById<View>(R.id.btnGuardarUbicacion) as Button
        this.btnGuardarRecorrido = findViewById<View>(R.id.btnGuardarRecorrido) as Button
        txtLatitud = findViewById(R.id.txtLatitud)
        txtLongitud = findViewById(R.id.txtLongitud)
        txtOrigen = findViewById(R.id.txtOrigen)
        txtDestino = findViewById(R.id.txtDestino)
        this.btnCerrarSesion = findViewById<View>(R.id.btnCerrarSesion) as Button
        /*mTextViewName = findViewById(R.id.textViewName)
        mTextViewEmail = findViewById(R.id.textViewEmail)*/

       /* fromPlaceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_OK -> {
                    val data: Intent? = result.data

                    if (data != null) {
                        val lugar = Autocomplete.getPlaceFromIntent(data)
                        Log.d(TAG, "PLace: ${lugar.name}")
                        try {
                            when (result.resultCode) {
                                REQUEST_CODE_AUTOCOMPLETE_FROM -> {
                                    handleAutocompleteResult(lugar, txtOrigen)
                                    Log.d(TAG, "PLace1: ${lugar.name}")
                                }
                                REQUEST_CODE_AUTOCOMPLETE_TO -> {
                                    handleAutocompleteResult(lugar, txtDestino)
                                    Log.d(TAG, "PLace2: ${lugar.name}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error setting text: ${e.message}")
                        }
                    }
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    val data: Intent? = result.data
                    val status = Autocomplete.getStatusFromIntent(data!!)
                    status.statusMessage.also {
                        showToast()
                    }
                }
                RESULT_CANCELED -> {
                    showToast()
                }
            }
        }*/

        /*fromPlaceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data

                if (data != null) {
                    val lugar = Autocomplete.getPlaceFromIntent(data)

                    try {
                        when (result.resultCode) {
                            REQUEST_CODE_AUTOCOMPLETE_FROM -> {
                                handleAutocompleteResult(lugar, txtOrigen)
                            }
                            REQUEST_CODE_AUTOCOMPLETE_TO -> {
                                handleAutocompleteResult(lugar, txtDestino)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting text: ${e.message}")
                    }
                }
            } else if (result.resultCode == AutocompleteActivity.RESULT_ERROR) {
                val data: Intent? = result.data
                val status = Autocomplete.getStatusFromIntent(data!!)
                status.statusMessage.also {
                    showToast()
                }
            } else if (result.resultCode == RESULT_CANCELED) {
                showToast()
            }
        }
*/
       /* fromPlaceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val requestCode = result.resultCode
                if(data != null) {
                    val lugar = Autocomplete.getPlaceFromIntent(data)
                    
                    
                    Log.d(TAG, "PLace: ${lugar.name}")

                    try {
                        txtOrigen.text = String.format("Ubicacion: %s", lugar.name)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting txtOrigen text: ${e.message}")
                    }

                    *//*fillInAddress(lugar)*//*
                }

                if (requestCode == REQUEST_CODE_AUTOCOMPLETE_FROM) {
                    // Manejar el resultado de la actividad de autocompletar "Desde"
                    val lugar = Autocomplete.getPlaceFromIntent(data!!)

                    Log.d(TAG, "Handling result for REQUEST_CODE_AUTOCOMPLETE_FROM")
                    Log.d(TAG, "Place details - Name: ${lugar.name}")
                    Log.d(TAG, "PLace: ${lugar.name}")

                    if (lugar.address != null) {
                        Log.d(TAG, "PLace: ${lugar.name}")
                    } else {
                        Log.d(TAG, "PLace: ${lugar.name}, Address not available")
                    }
                    //Esto ayudará a evitar que se produzcan excepciones si la dirección es nula y permitirá un mejor manejo de los datos en tu aplicación.

                    try {
                        txtOrigen.text = String.format("Ubicacion de origen: %s", lugar.name)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting txtOrigen text: ${e.message}")
                    }

                    // Hacer algo con los datos seleccionados de "Desde"
                } else if (requestCode == REQUEST_CODE_AUTOCOMPLETE_TO) {
                    val lugar = Autocomplete.getPlaceFromIntent(data!!)
                    result.data
                    result.resultCode
                    lugar.name

                    try {
                        txtDestino.text = String.format("Ubicacion Destino: %s", lugar.name)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting txtOrigen text: ${e.message}")
                    }
                    // Manejar el resultado de la actividad de autocompletar "Hacia" si es necesario
                    // Realizar acciones para el resultado de "Hacia" si es necesario
                }
            } else if (result.resultCode == AutocompleteActivity.RESULT_ERROR) {
                val data: Intent? = result.data
                val status = Autocomplete.getStatusFromIntent(data!!)

                status.statusMessage.also {

                    showToast()
                }

                // Manejar errores
            } else if (result.resultCode == RESULT_CANCELED) {
                // El usuario canceló la selección
                showToast()
            }
        }*/

        val mAuth = FirebaseAuth.getInstance()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync { googleMap ->
            mMap = googleMap

            val opcionesMapa: UiSettings = googleMap.uiSettings
            opcionesMapa.isZoomControlsEnabled = true
            opcionesMapa.isCompassEnabled = true

            val zoomLevel = 15f

            val chile = LatLng(-33.4323276, -70.6335647)
            mMap.addMarker(MarkerOptions().position(chile).title(getString(R.string.marker_chile)))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(chile,zoomLevel))

            mMap.setOnMapClickListener(this)
            mMap.setOnMapLongClickListener(this)

            enableLocation()

            }

            // Initialize the SDK
            Places.initialize(applicationContext, getString(R.string.android_sdk_api_key))

            // Create a new PlacesClient instance


            // Inicializa Firebase Realtime Database
            databaseReference = FirebaseDatabase.getInstance().reference.child("ubicaciones")

            // Inicializa FusedLocationProviderClient para obtener la ubicación
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            btnGuardarUbicacion.setOnClickListener {
                guardarUbicacion()
            }

            btnGuardarRecorrido.setOnClickListener {
                guardarRecorrido()
            }

            btnCerrarSesion.setOnClickListener {
                mAuth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }

            btnOrigen.setOnClickListener {
                /*startAutocomplete()*/
                startAutocompleteForOrigen()
            }

            btnDestino.setOnClickListener {
                /*startAutocomplete()*/
                startAutocompleteForDestino()
            }

            
            getUserInfo()
        }

  /*  private fun handleAutocompleteResult(lugar: Place, textView: TextView) {
        Log.d(TAG, "Handling result: ${lugar.name}")

        if (lugar.address != null) {
            Log.d(TAG, "Address: ${lugar.name}")
        } else {
            Log.d(TAG, "Address not available")
        }

        textView.text = String.format("Ubicacion: %s, ${lugar.name}")
    }*/


   /* private fun startAutocomplete() {
        Log.d(TAG, "Buscando una dirección en la API Places")
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(
            this@MainActivity
        )

        // Launch the Autocomplete activity with the custom launcher
        fromPlaceLauncher.launch(intent)
    }*/

    @SuppressLint("SetTextI18n")
    private fun handleOrigenSelection(lugar: Place) {
        origenLocation = lugar.latLng
        txtOrigen.text = "Origen: ${lugar.name}"
        agregarMarcadorDeOrigen()
        ubicacionesSeleccionadas(lugar)
        val origenLatitud = lugar.latLng?.latitude ?: 0.0
        val origenLongitud = lugar.latLng?.longitude ?: 0.0

        // Obtén la dirección del lugar de origen
        val origenDireccion = lugar.address

        // Almacena la ubicación y dirección de origen en Firebase
        guardarRecorridoFirebase(origenLatitud, origenLongitud, origenDireccion, null, null, null)
    }

    private fun agregarMarcadorDeOrigen() {
        if (origenLocation != null) {
            val latLngOrigen = LatLng(origenLocation!!.latitude, origenLocation!!.longitude)
            mMap.addMarker(MarkerOptions().position(latLngOrigen).title("Origen"))
        }
    }

    @SuppressLint("SetTextI18n")
    private fun handleDestinoSelection(lugar: Place) {
        destinoLocation = lugar.latLng
        txtDestino.text = "Destino: ${lugar.name}"
        ubicacionesSeleccionadas(lugar)

        val destinoLatitud = lugar.latLng?.latitude ?: 0.0
        val destinoLongitud = lugar.latLng?.longitude ?: 0.0

        // Obtén la dirección del lugar de destino
        val destinoDireccion = lugar.address

        // Almacena la ubicación y dirección de destino en Firebase
        guardarRecorridoFirebase(null, null, null, destinoLatitud, destinoLongitud, destinoDireccion)
    }

    private fun startAutocompleteForOrigen() {
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(this@MainActivity)

        // Utiliza el lanzador correspondiente para el botón de origen
        fromPlaceLauncherForOrigen.launch(intent)
    }

    private fun startAutocompleteForDestino() {
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(this@MainActivity)

        // Utiliza el lanzador correspondiente para el botón de destino
        fromPlaceLauncherForDestino.launch(intent)
    }

    override fun onMapClick(latLng: LatLng) {
        updateMarkerAndCamera(latLng)
    }

    override fun onMapLongClick(latLng: LatLng) {
        updateMarkerAndCamera(latLng)
    }

    private fun updateMarkerAndCamera(latLng: LatLng) {
        txtLatitud.setText(latLng.latitude.toString())
        txtLongitud.setText(latLng.longitude.toString())

        mMap.clear()

        val markerOptions = MarkerOptions().position(latLng).title(getString(R.string.marker_new))
        mMap.addMarker(markerOptions)

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    override fun onMapReady(p0: GoogleMap) {
        TODO("Not yet implemented")

    }

    private fun isLocatedPermissionGranted() =
        checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun enableLocation() {
        if (!::mMap.isInitialized) return
        if (isLocatedPermissionGranted()) {
            if (checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            mMap.isMyLocationEnabled = true


        } else {
            requestLocationPermission()
        }
    }

    private fun showToast(message: String?) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun guardarUbicacion() {
        // Aquí obtén la latitud y longitud actual del mapa
        val latitud = mMap.cameraPosition.target.latitude
        val longitud = mMap.cameraPosition.target.longitude

        // Luego, guarda esta ubicación en Firebase Realtime Database u otro lugar de almacenamiento.
        guardarUbicacionEnFirebase(latitud, longitud)

        // Puedes mostrar un mensaje de confirmación después de guardar la ubicación
        Toast.makeText(this, "Ubicación guardada con éxito", Toast.LENGTH_SHORT).show()
    }

    private fun guardarRecorrido() {
        val origenLatitud = origenLocation?.latitude
        val origenLongitud = origenLocation?.longitude
        val origenDireccion = txtOrigen.text.toString()
        val destinoLatitud = destinoLocation?.latitude
        val destinoLongitud = destinoLocation?.longitude
        val destinoDireccion = txtDestino.text.toString()

        guardarRecorridoFirebase(origenLatitud, origenLongitud,origenDireccion, destinoLatitud,destinoLongitud,destinoDireccion)

        Toast.makeText(this, "Recorrido guardado con éxito", Toast.LENGTH_SHORT).show()

    }

    private fun guardarUbicacionEnFirebase(latitud: Double, longitud: Double) {
        // Crear un objeto de ubicación para guardar en la base de datos
        val ubicacion = Ubicacion(latitud, longitud)

        // Generar una clave única para la ubicación
        val nuevaUbicacionKey = databaseReference.push().key

        if (nuevaUbicacionKey != null) {
            // Guardar la ubicación en la base de datos con la clave generada
            databaseReference.child(nuevaUbicacionKey).setValue(ubicacion)

        }
    }

    private fun guardarRecorridoFirebase(origenLatitud: Double?,
                                         origenLongitud: Double?,
                                         origenDireccion: String?,
                                         destinoLatitud: Double?,
                                         destinoLongitud: Double?,
                                         destinoDireccion: String?) {

        // Crea objetos para guardar en la base de datos
        val ubicacionOrigen = Recorrido(origenLatitud, origenLongitud, origenDireccion)
        val ubicacionDestino = Recorrido(destinoLatitud, destinoLongitud, destinoDireccion)

        // Genera claves únicas para las ubicaciones de origen y destino
        val nuevoRecorridoOrigenKey = databaseReference.push().key
        val nuevoRecorridoDestinoKey = databaseReference.push().key

        if (nuevoRecorridoOrigenKey != null && nuevoRecorridoDestinoKey != null) {
            // Guarda la ubicación de origen en la base de datos
            databaseReference.child(nuevoRecorridoOrigenKey).setValue(ubicacionOrigen)

            // Guarda la ubicación de destino en la base de datos
            databaseReference.child(nuevoRecorridoDestinoKey).setValue(ubicacionDestino)
        }
    }

    data class Recorrido(
        val latitud: Double?,
        val longitud: Double?,
        val direccion: String? = null
    )

    data class Ubicacion(val latitud: Double, val longitud: Double)


    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
        ) {
            Toast.makeText(this, "Ve a ajustes y acepta los permisos", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        when (requestCode) {
            REQUEST_CODE_LOCATION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mMap.isMyLocationEnabled = true
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Para activar la localizacion ve a ajustes y acepta los permisos",
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {

            }
        }
    }
    private fun getUserInfo() {
        val mAuth = FirebaseAuth.getInstance()
        val id = mAuth.currentUser!!.uid
        val mDatabase = FirebaseDatabase.getInstance().reference.child("Users")
        mDatabase.child("Users").child(id).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val name = dataSnapshot.child("name").getValue<String>()
                    val email = dataSnapshot.child("email").getValue<String>()

                    mTextViewName.text = name
                    mTextViewEmail.text = email
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Manejar la cancelación de la lectura de datos aquí
            }
        })
    }

}









