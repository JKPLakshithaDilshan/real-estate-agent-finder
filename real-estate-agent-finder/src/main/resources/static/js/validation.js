    document.addEventListener('DOMContentLoaded', () => {
    const phonePattern = /^[+0-9()\-\s]{7,20}$/;
    const phoneTenDigitsPattern = /^\d{10}$/;
    const nicIdPattern = /^(?:\d{12}|\d{9}[Vv])$/;
    const recordIdPattern = /^[A-Za-z0-9_-]{2,30}$/;
    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    const passwordRules = value => ({
        length: value.length >= 8,
        uppercase: /[A-Z]/.test(value),
        lowercase: /[a-z]/.test(value),
        number: /\d/.test(value),
        special: /[^A-Za-z0-9]/.test(value)
    });

    const strengthMeta = score => {
        if (score <= 1) {
            return { label: 'Too weak', width: '20%', color: '#dc2626' };
        }
        if (score === 2) {
            return { label: 'Weak', width: '40%', color: '#ea580c' };
        }
        if (score === 3) {
            return { label: 'Fair', width: '60%', color: '#ca8a04' };
        }
        if (score === 4) {
            return { label: 'Strong', width: '80%', color: '#0284c7' };
        }
        return { label: 'Very strong', width: '100%', color: '#16a34a' };
    };

    const showFieldError = (input, message) => {
        input.classList.add('is-invalid');
        const fieldError = input.closest('.col-12, .col-md-6, .col-md-12, .form-col-6, .form-col-12, .form-col-4, .form-col-8')
            ?.querySelector(`[data-error-for="${input.name || input.id}"]`);
        if (fieldError) {
            fieldError.textContent = message;
            fieldError.classList.add('is-visible');
        }
    };

    const clearFieldError = input => {
        input.classList.remove('is-invalid');
        const fieldError = input.closest('.col-12, .col-md-6, .col-md-12, .form-col-6, .form-col-12, .form-col-4, .form-col-8')
            ?.querySelector(`[data-error-for="${input.name || input.id}"]`);
        fieldError?.classList.remove('is-visible');
    };

    const validateInput = input => {
        const rawValue = input.value;
        if (input.dataset.trim === 'true') {
            input.value = rawValue.trim();
        }
        const value = input.value;

        if (input.hasAttribute('required') && input.type === 'checkbox' && !input.checked) {
            showFieldError(input, 'This field is required.');
            return false;
        }

        if (input.hasAttribute('required') && input.type !== 'checkbox' && !value) {
            showFieldError(input, 'This field is required.');
            return false;
        }

        if (!value) {
            clearFieldError(input);
            return true;
        }

        if (input.type === 'email' && !emailPattern.test(value)) {
            showFieldError(input, 'Enter a valid email address.');
            return false;
        }

        if (input.dataset.phoneTenDigits === 'true' && !phoneTenDigitsPattern.test(value)) {
            showFieldError(input, 'Phone number must contain exactly 10 digits.');
            return false;
        }

        if (input.dataset.phone === 'true' && !phonePattern.test(value)) {
            showFieldError(input, 'Enter a valid phone number.');
            return false;
        }

        if (input.dataset.nicId === 'true' && !nicIdPattern.test(value)) {
            showFieldError(input, 'NIC/ID must be 12 digits or 9 digits followed by V.');
            return false;
        }

        if (input.dataset.recordId === 'true' && !recordIdPattern.test(value)) {
            showFieldError(input, 'Use 2-30 characters: letters, numbers, underscore, or hyphen.');
            return false;
        }

        if (input.minLength > 0 && value.length < input.minLength) {
            showFieldError(input, `Minimum ${input.minLength} characters required.`);
            return false;
        }

        if (input.maxLength > 0 && value.length > input.maxLength) {
            showFieldError(input, `Maximum ${input.maxLength} characters allowed.`);
            return false;
        }

        if (input.type === 'number') {
            const numericValue = Number(value);
            if (input.min !== '' && numericValue < Number(input.min)) {
                showFieldError(input, `Value must be at least ${input.min}.`);
                return false;
            }
            if (input.max !== '' && numericValue > Number(input.max)) {
                showFieldError(input, `Value must be no more than ${input.max}.`);
                return false;
            }
        }

        if (input.type === 'date' && input.dataset.futureDate === 'true') {
            const selectedDate = new Date(value);
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            if (selectedDate < today) {
                showFieldError(input, 'Please choose a current or future date.');
                return false;
            }
        }

        if (input.dataset.passwordStrength === 'true') {
            const rules = passwordRules(value);
            const passedCount = Object.values(rules).filter(Boolean).length;
            if (passedCount < 5) {
                showFieldError(input, 'Password must include uppercase, lowercase, number, and special character.');
                return false;
            }
        }

        if (input.dataset.confirmPassword) {
            const target = document.getElementById(input.dataset.confirmPassword);
            if (target && value !== target.value) {
                showFieldError(input, 'Confirm password must match the password.');
                return false;
            }
        }

        clearFieldError(input);
        return true;
    };

    document.querySelectorAll('[data-password-toggle]').forEach(toggle => {
        toggle.addEventListener('click', () => {
            const target = document.getElementById(toggle.dataset.passwordToggle);
            if (!target) {
                return;
            }
            const isPassword = target.type === 'password';
            target.type = isPassword ? 'text' : 'password';
            const icon = toggle.querySelector('i');
            if (icon) {
                icon.className = isPassword ? 'fa-regular fa-eye-slash' : 'fa-regular fa-eye';
            }
        });
    });

    document.querySelectorAll('[data-password-strength="true"]').forEach(input => {
        const strengthWrapper = document.querySelector(`[data-strength-for="${input.id}"]`);
        const fill = strengthWrapper?.querySelector('.password-strength-fill');
        const label = strengthWrapper?.querySelector('[data-strength-label]');
        const checklist = document.querySelector(`[data-password-checklist="${input.id}"]`);

        const renderStrength = () => {
            const rules = passwordRules(input.value);
            const score = Object.values(rules).filter(Boolean).length;
            const meta = strengthMeta(score);

            if (fill) {
                fill.style.width = meta.width;
                fill.style.backgroundColor = meta.color;
            }
            if (label) {
                label.textContent = meta.label;
            }

            checklist?.querySelectorAll('[data-rule]').forEach(item => {
                const passed = rules[item.dataset.rule];
                item.classList.toggle('is-valid', passed);
                item.classList.toggle('is-invalid', !passed && input.value.length > 0);
            });
        };

        input.addEventListener('input', renderStrength);
        renderStrength();
    });

    document.querySelectorAll('form').forEach(form => {
        const fields = form.querySelectorAll('input, textarea, select');
        fields.forEach(input => {
            input.addEventListener('input', () => validateInput(input));
            input.addEventListener('blur', () => validateInput(input));
        });

        form.addEventListener('submit', event => {
            let valid = true;
            fields.forEach(input => {
                if (!validateInput(input)) {
                    valid = false;
                }
            });

            if (!valid) {
                event.preventDefault();
                const firstInvalid = form.querySelector('.is-invalid');
                firstInvalid?.focus();
                return;
            }

            const submitButton = form.querySelector('button[type="submit"]');
            if (submitButton) {
                submitButton.disabled = true;
                submitButton.dataset.originalText = submitButton.innerHTML;
                submitButton.innerHTML = 'Please wait...';
                window.setTimeout(() => {
                    submitButton.disabled = false;
                    submitButton.innerHTML = submitButton.dataset.originalText || submitButton.innerHTML;
                }, 4000);
            }
        });
    });
});
