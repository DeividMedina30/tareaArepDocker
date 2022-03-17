function subirPalabra() {
    var cadena = document.getElementById("idtextopalabra").value;
    document.getElementById("idtextopalabra").value = "";
    var url = "/balancer?value=" + cadena;
    fetch(url,{
        method: 'POST',
        mode: 'cors',
        cache: 'no-cache',
        credentials: 'same-origin',
      }).then(res=>res.json()).then(function (data) {
        var tabla = document.getElementById("tablaMongo");
        var filaLong = tabla.rows.length;
        for (var x = filaLong - 1; x > 0; x--) {
            tabla.deleteRow(x);
        }
        data.forEach(element => { //ejecuta la función indicada una vez por cada elemento del array.
            let row = tabla.insertRow();
            let fecha = row.insertCell();
            let cadena = row.insertCell();
            fecha.innerHTML = element.fecha;
            cadena.innerHTML = element.value;
        });
    });
}