window.addEventListener("DOMContentLoaded", (e) => {
    const deliveryVehicleIdInput = document.getElementById("delivery-vehicle-id-input");

    const vehicleRows = document.querySelectorAll("ul.vehicleChooser li.mdc-deprecated-list-item");
    vehicleRows.forEach(row => {
        const radio = row.querySelector("input.mdc-radio__native-control");
        row.addEventListener('click', (e) => {
            // Material UI is supposed to do this for us, but it doesn't seem to work. This is fine for a demo / POC but ideally we should debug it properly.
            radio.checked = true;
            const event = new Event('change');
            radio.dispatchEvent(event);
        });
        radio.addEventListener('change', (e) => {
            if (radio.checked) {
                deliveryVehicleIdInput.focus();
                window.currentVehicle = radio.dataset.name;
                window.currentVehicleUi = radio.dataset.uiname;
                deliveryVehicleIdInput.value = radio.dataset.uiname;
                deliveryVehicleIdInput.dispatchEvent(new KeyboardEvent('keyup',{'key':'Enter'}));
                deliveryVehicleIdInput.blur();
                radio.focus();

                updateDeliveryVehicleSpeeding();
                const urlParams = new URLSearchParams(window.location.search);
                const pollRateMs = urlParams.get('pollingIntervalMillis') ?? 5000;
                window.setInterval(updateDeliveryVehicleSpeeding, pollRateMs);
            }
        });
    });

    window.deliveryVehicleMarkers = {};
    window.setInterval(updateDeliveryVehicleTracking, 1000 * 60);
    updateDeliveryVehicleTracking();

    // Ensure the task list and map view load data for the correct vehicle upon page load.
    const urlParams = new URLSearchParams(window.location.search);
    let preselectedVehicle = document.getElementById(`row_${urlParams.get('deliveryVehicleId')}`);
    if (!preselectedVehicle) {
        preselectedVehicle = document.querySelector("ul.vehicleChooser li");
    }
    preselectedVehicle?.click();
});

const updateDeliveryVehicleTracking = () => {
    fetch("/fleet/vehicles")
        .then((response) => response.json())
        .catch((e) => {
            console.error('window.deliveryVehiclesJson defaulted to empty array due to json parse error', e);
            return [];
        }).then((deliveryVehiclesJson) => {
            // console.log(deliveryVehiclesJson);

            document.getElementById("vehicleChooserHeading").innerText = `Fleet of Vehicles (${deliveryVehiclesJson.length})`;

            window.deliveryVehiclesJson = deliveryVehiclesJson;
            window.deliveryVehiclesJson.forEach((deliveryVehicle, i) => {
                // console.log(deliveryVehicle, i);
                if (deliveryVehicle.name_ === window.currentVehicle){
                    return;
                }
                if (deliveryVehicle.lastLocation_ && deliveryVehicle.lastLocation_.location_) {
                    let marker = window.deliveryVehicleMarkers[deliveryVehicle.name_];
                    if (!marker) {
                        marker = new google.maps.Marker({map: window.deliveryVehicleTrackingApp.mapView.map, icon: {
                            path: 'M12 2L4.5 20.29l.71.71L12 18l6.79 3 .71-.71z',
                            fillOpacity: 0.9,
                            scale: 1.2,
                            fillColor: '#bababa',
                            anchor: {x: 12, y: 12},
                        }});
                    }
                    marker.setPosition({
                        lat: (i * 0.0001) + deliveryVehicle.lastLocation_.location_.latitude_,
                        lng: deliveryVehicle.lastLocation_.location_.longitude_
                    });
                    const lastUpdate = deliveryVehicle.lastLocation_.serverTime_ ? 
                        moment(deliveryVehicle.lastLocation_.serverTime_.seconds_ * 1000).fromNow() : "Unknown";
                    // console.log('Setting title to', lastUpdate);
                    marker.setTitle(lastUpdate);
                // } else {
                //     console.log('No location for', deliveryVehicle);
                }
            });          
        });
};

const updateDeliveryVehicleSpeeding = () => {
    fetch("/speeding")
        .then((response) => response.json())
        .catch((e) => {
            console.error('Speeding info unavailable due to json parse error', e);
            return {};
        }).then((speedingInfo) => {
            console.log(speedingInfo);

            const vehicleSpeedingInfo = speedingInfo[window.currentVehicleUi];

            let speedingSeverity = 'none';

            const infoBox = document.getElementById("vehicleSpeedingInfo");
            infoBox.classList.remove("speeding-major");
            infoBox.classList.remove("speeding-minor");
            infoBox.classList.remove("speeding-none");

            if (vehicleSpeedingInfo){
                vehicleSpeedingInfo.sort((a, b) => {
                    return a.timestamp - b.timestamp;
                });
                const vehicleSpeedingData = vehicleSpeedingInfo.pop();

                const ts1000 = new Date(vehicleSpeedingData.timestamp * 1000);
                const ts1 = new Date(vehicleSpeedingData.timestamp);
                let ts = ts1000.getFullYear() >= 3000 ? ts1 : ts1000;

                speedingSeverity = vehicleSpeedingData?.speedAlertSeverity?.toLowerCase() ?? 'none';
                const timeString = moment(ts).fromNow();
                if (speedingSeverity === 'none') {
                    infoBox.innerText = `This vehicle is driving within the speed limit. Last updated ${timeString}`;
                    infoBox.title = ts.toLocaleString();
                } else {
                    infoBox.innerText = `This vehicle was ${vehicleSpeedingData.percentageAboveLimit}% above the speed limit. Last updated ${timeString}`;
                }
            } else {
                infoBox.innerText = "Speeding data currently unavailable";
            }

            infoBox.classList.add(`speeding-${speedingSeverity}`);
        });
};