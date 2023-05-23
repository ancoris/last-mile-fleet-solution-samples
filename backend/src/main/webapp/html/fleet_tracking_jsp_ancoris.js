window.addEventListener("DOMContentLoaded", (e) => {
    const deliveryVehicleIdInput = document.getElementById("delivery-vehicle-id-input");

    const vehicleRows = document.querySelectorAll("ul.vehicleChooser li.mdc-deprecated-list-item");
    vehicleRows.forEach(row => {
        const radio = row.querySelector("input.mdc-radio__native-control");
        row.addEventListener('click', (e) => {
            radio.checked = true;
            const event = new Event('change');
            radio.dispatchEvent(event);
        });
        radio.addEventListener('change', (e) => {
            if (radio.checked) {
                deliveryVehicleIdInput.focus();
                deliveryVehicleIdInput.value = radio.dataset.uiname;
                deliveryVehicleIdInput.dispatchEvent(new KeyboardEvent('keyup',{'key':'Enter'}));
                deliveryVehicleIdInput.blur();
                radio.focus();
            }
        });
    });
});
