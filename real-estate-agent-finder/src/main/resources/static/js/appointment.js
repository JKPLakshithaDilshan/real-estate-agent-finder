// appointment.js - Appointment date/time helpers

document.addEventListener('DOMContentLoaded', () => {
    const dateInput = document.querySelector('input[type="datetime-local"]');
    if (dateInput) {
        // Set minimum date to today
        const now = new Date();
        now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
        dateInput.min = now.toISOString().slice(0, 16);
    }
});
