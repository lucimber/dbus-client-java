// Version switcher for Javadoc pages
(function() {
    // Only run on Javadoc pages
    if (!document.title.includes('API') && !document.querySelector('.header .title')) return;
    
    // Create version switcher container
    const switcher = document.createElement('div');
    switcher.id = 'version-switcher';
    switcher.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        z-index: 1000;
        background: #fff;
        border: 2px solid #ddd;
        border-radius: 8px;
        padding: 10px;
        box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        font-size: 14px;
    `;
    
    // Create select element
    const select = document.createElement('select');
    select.style.cssText = `
        padding: 5px 10px;
        border: 1px solid #ccc;
        border-radius: 4px;
        background: white;
        min-width: 120px;
    `;
    
    // Get current version from path
    const pathParts = window.location.pathname.split('/');
    const currentVersion = pathParts.find(part => part.match(/^(v\d+\.\d+\.\d+|latest)$/)) || 'latest';
    
    // Add label
    const label = document.createElement('label');
    label.textContent = 'Version: ';
    label.style.marginRight = '8px';
    
    switcher.appendChild(label);
    switcher.appendChild(select);
    
    // Load versions and populate select
    fetch('/versions.json')
        .then(response => response.json())
        .then(data => {
            data.versions.forEach(version => {
                const option = document.createElement('option');
                option.value = version.name;
                option.textContent = version.title;
                option.selected = version.name === currentVersion;
                select.appendChild(option);
            });
        })
        .catch(error => {
            console.error('Error loading versions:', error);
            // Fallback options
            ['latest', 'v1.0.0'].forEach(version => {
                const option = document.createElement('option');
                option.value = version;
                option.textContent = version === 'latest' ? 'Latest' : version;
                option.selected = version === currentVersion;
                select.appendChild(option);
            });
        });
    
    // Handle version switching
    select.addEventListener('change', function() {
        const selectedVersion = this.value;
        if (selectedVersion !== currentVersion) {
            // Get the current relative path within the javadoc
            const currentPath = window.location.pathname;
            const javadocIndex = currentPath.indexOf('/' + currentVersion + '/');
            
            if (javadocIndex !== -1) {
                // Replace the version in the path
                const beforeVersion = currentPath.substring(0, javadocIndex);
                const afterVersion = currentPath.substring(javadocIndex + currentVersion.length + 1);
                const newPath = beforeVersion + '/' + selectedVersion + '/' + afterVersion;
                window.location.href = newPath;
            } else {
                // Fallback: go to root of selected version
                const basePath = window.location.origin + window.location.pathname.split('/').slice(0, -currentPath.split('/').slice(1).length + 1).join('/');
                window.location.href = basePath + '/' + selectedVersion + '/';
            }
        }
    });
    
    // Add to page
    document.body.appendChild(switcher);
    
    // Add back to main documentation link
    const backLink = document.createElement('div');
    backLink.style.cssText = `
        position: fixed;
        top: 20px;
        left: 20px;
        z-index: 1000;
        background: #667eea;
        color: white;
        padding: 8px 16px;
        border-radius: 6px;
        box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        font-size: 14px;
    `;
    
    backLink.innerHTML = '<a href="/" style="color: white; text-decoration: none;">← Documentation Home</a>';
    document.body.appendChild(backLink);
})();