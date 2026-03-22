document.addEventListener('DOMContentLoaded', () => {
    const body = document.body;
    let pageLoader;

    const hidePageLoader = () => {
        if (!pageLoader) {
            return;
        }
        pageLoader.classList.add('hidden');
        window.setTimeout(() => pageLoader.remove(), 420);
    };

    window.addEventListener('load', hidePageLoader);
    window.setTimeout(hidePageLoader, 1400);

    // Insert shared UX layer if a page did not include fragment-level layer.
    const ensureUxLayer = () => {
        if (document.getElementById('globalToastStack') && document.getElementById('globalBusyOverlay')) {
            return;
        }
        const layer = document.createElement('div');
        layer.className = 'global-ux-layer';
        layer.setAttribute('aria-live', 'polite');
        layer.setAttribute('aria-atomic', 'true');
        layer.innerHTML = [
            '<div class="page-loader" id="pageLoader" aria-hidden="true">',
            '  <div class="page-loader-inner">',
            '    <span class="loader-pulse" aria-hidden="true"></span>',
            '    <p class="loader-label">Loading Experience</p>',
            '  </div>',
            '</div>',
            '<div class="busy-overlay" id="globalBusyOverlay" hidden>',
            '  <div class="busy-shell" role="status" aria-label="Processing">',
            '    <span class="busy-spinner" aria-hidden="true"></span>',
            '    <span class="busy-label">Working on your request...</span>',
            '  </div>',
            '</div>',
            '<div class="toast-stack" id="globalToastStack"></div>'
        ].join('');
        body.appendChild(layer);
    };

    ensureUxLayer();
    pageLoader = document.getElementById('pageLoader');

    const busyOverlay = document.getElementById('globalBusyOverlay');
    const toastStack = document.getElementById('globalToastStack');

    const showBusy = (labelText) => {
        if (!busyOverlay) {
            return;
        }
        const label = busyOverlay.querySelector('.busy-label');
        if (label && labelText) {
            label.textContent = labelText;
        }
        busyOverlay.hidden = false;
    };

    const hideBusy = () => {
        if (!busyOverlay) {
            return;
        }
        busyOverlay.hidden = true;
    };

    const toastMeta = type => {
        switch (type) {
            case 'success':
                return { className: 'site-toast-success', icon: 'fa-solid fa-circle-check' };
            case 'error':
                return { className: 'site-toast-error', icon: 'fa-solid fa-circle-xmark' };
            case 'warning':
                return { className: 'site-toast-warning', icon: 'fa-solid fa-triangle-exclamation' };
            default:
                return { className: 'site-toast-info', icon: 'fa-solid fa-circle-info' };
        }
    };

    const createToast = (message, type = 'info', timeout = 4200) => {
        if (!toastStack || !message || !message.trim()) {
            return;
        }

        const meta = toastMeta(type);
        const toast = document.createElement('div');
        toast.className = `site-toast ${meta.className}`;
        toast.setAttribute('role', type === 'error' ? 'alert' : 'status');
        toast.innerHTML = [
            `<span class="site-toast-icon"><i class="${meta.icon}"></i></span>`,
            `<div class="site-toast-content">${message}</div>`,
            '<button type="button" class="site-toast-close" aria-label="Dismiss notification">',
            '  <i class="fa-solid fa-xmark"></i>',
            '</button>'
        ].join('');

        const dismiss = () => {
            toast.classList.remove('show');
            window.setTimeout(() => toast.remove(), 180);
        };

        toast.querySelector('.site-toast-close')?.addEventListener('click', dismiss);
        toastStack.appendChild(toast);

        requestAnimationFrame(() => toast.classList.add('show'));
        window.setTimeout(dismiss, timeout);
    };

    // Expose globally for page-specific scripts.
    window.SiteUx = {
        showBusy,
        hideBusy,
        toast: createToast
    };

    // Sticky navbar shadow on scroll.
    const navbar = document.querySelector('.site-nav');
    if (navbar) {
        window.addEventListener('scroll', () => {
            navbar.classList.toggle('scrolled', window.scrollY > 10);
        });
    }

    // Auto-reveal dense page sections for a modern premium feel.
    const revealTargets = document.querySelectorAll([
        '.table-card',
        '.appointment-mobile-card',
        '.empty-state',
        '.surface-card',
        '.surface-panel',
        '.detail-card',
        '.toolbar-card',
        '.agent-card',
        '.top-rated-card',
        '.review-card',
        '.why-card',
        '.process-card',
        '.cta-panel',
        '.metric-card',
        '.management-panel',
        '.management-toolbar',
        '.directory-summary',
        '.filter-card',
        '.search-filter-card',
        '.sort-summary-card',
        '.rating-summary-card',
        '.review-stack-premium',
        '.profile-shell',
        '.appointment-form-card',
        '.admin-panel-card',
        '.auth-shell'
    ].join(','));

    revealTargets.forEach((el, index) => {
        el.classList.add('reveal-init');
        el.style.transitionDelay = `${Math.min(index * 40, 280)}ms`;
    });

    if (revealTargets.length > 0 && 'IntersectionObserver' in window) {
        const revealObserver = new IntersectionObserver(entries => {
            entries.forEach(entry => {
                if (!entry.isIntersecting) {
                    return;
                }
                entry.target.classList.add('reveal-in');
                revealObserver.unobserve(entry.target);
            });
        }, {
            threshold: 0.12,
            rootMargin: '0px 0px -48px 0px'
        });

        revealTargets.forEach(el => revealObserver.observe(el));
    } else {
        revealTargets.forEach(el => el.classList.add('reveal-in'));
    }

    // Mobile nav handling.
    const toggle = document.getElementById('navToggle');
    const navLinks = document.getElementById('navLinks');
    if (toggle && navLinks) {
        navLinks.querySelectorAll('.nav-link').forEach(link => {
            link.addEventListener('click', () => {
                if (window.bootstrap?.Collapse) {
                    const instance = window.bootstrap.Collapse.getInstance(navLinks)
                        || new window.bootstrap.Collapse(navLinks, { toggle: false });
                    instance.hide();
                }
            });
        });
    }

    // Highlight active nav link by path.
    const currentPath = window.location.pathname.replace(/\/$/, '') || '/';
    document.querySelectorAll('.nav-link[href]').forEach(link => {
        const href = link.getAttribute('href');
        if (!href || href.startsWith('#')) {
            return;
        }
        const normalizedHref = href.replace(/\/$/, '') || '/';
        if (normalizedHref === currentPath) {
            document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
            link.classList.add('active');
        }
    });

    // Convert page-level alerts into toasts while keeping alerts as accessible fallback.
    document.querySelectorAll('.alert-success, .alert-danger, .alert-warning').forEach(alert => {
        const text = (alert.textContent || '').trim();
        if (!text) {
            return;
        }
        if (alert.dataset.toastShown === 'true') {
            return;
        }
        alert.dataset.toastShown = 'true';
        const type = alert.classList.contains('alert-danger')
            ? 'error'
            : alert.classList.contains('alert-warning')
                ? 'warning'
                : 'success';
        createToast(text, type);
    });

    // Render lightweight flash toasts from payload-only elements.
    document.querySelectorAll('[data-toast-message]').forEach(toastPayload => {
        if (toastPayload.dataset.toastShown === 'true') {
            return;
        }
        const message = (toastPayload.dataset.toastMessage || '').trim();
        if (!message) {
            return;
        }
        const type = (toastPayload.dataset.toastType || 'success').trim();
        toastPayload.dataset.toastShown = 'true';
        createToast(message, type);
    });

    // Global submit busy state with duplicate submission prevention.
    document.querySelectorAll('form').forEach(form => {
        if (form.dataset.manualSubmitUx === 'true') {
            return;
        }

        form.addEventListener('submit', event => {
            if (form.dataset.submitting === 'true') {
                event.preventDefault();
                return;
            }

            form.dataset.submitting = 'true';
            showBusy('Submitting your request...');

            const submitButton = form.querySelector('button[type="submit"]');
            if (submitButton) {
                submitButton.disabled = true;
                if (!submitButton.dataset.originalText) {
                    submitButton.dataset.originalText = submitButton.innerHTML;
                }
                submitButton.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-1"></i>Please wait...';
            }

            window.setTimeout(() => {
                if (form.dataset.submitting === 'true') {
                    form.dataset.submitting = 'false';
                    hideBusy();
                    if (submitButton) {
                        submitButton.disabled = false;
                        submitButton.innerHTML = submitButton.dataset.originalText || submitButton.innerHTML;
                    }
                }
            }, 6000);
        });
    });

    window.addEventListener('pageshow', () => {
        hideBusy();
        document.querySelectorAll('form[data-submitting="true"]').forEach(form => {
            form.dataset.submitting = 'false';
            const submitButton = form.querySelector('button[type="submit"]');
            if (submitButton && submitButton.dataset.originalText) {
                submitButton.disabled = false;
                submitButton.innerHTML = submitButton.dataset.originalText;
            }
        });
    });

    // Auto-apply dense action behavior for action-heavy wrappers.
    document.querySelectorAll('.d-flex.gap-2.flex-wrap').forEach(wrapper => {
        if (wrapper.querySelector('.btn-sm') || wrapper.querySelector('form')) {
            wrapper.classList.add('dense-actions');
        }
    });
});
