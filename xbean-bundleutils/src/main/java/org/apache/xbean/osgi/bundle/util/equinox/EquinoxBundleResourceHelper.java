/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.xbean.osgi.bundle.util.equinox;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.apache.xbean.osgi.bundle.util.BundleResourceHelper;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Equinox implementation of BundleResourceHelper.
 * <br/>
 * This implementation of the {@link BundleResourceHelper} converts resource URLs
 * returned by {@link #getResource(String)} and {@link #getResources(String)} into <tt>jar</tt> URLs
 * using Equinox's <tt>URLConverter</tt> service.
 * 
 * @version $Rev$ $Date$
 */
public class EquinoxBundleResourceHelper extends BundleResourceHelper {

    private URLConverter converter;
    
    public EquinoxBundleResourceHelper(Bundle bundle, boolean searchWiredBundles, boolean convertResourceUrls) {
        super(bundle, searchWiredBundles, convertResourceUrls);
        init();
    }

    private void init() {
        BundleContext context = bundle.getBundleContext();
        if (context != null) {
            ServiceReference urlReference = context.getServiceReference(URLConverter.class.getName());
            if (urlReference != null) { 
                converter = (URLConverter) context.getService(urlReference);
            }
        }
    }
    
    @Override
    public URL getResource(String name) {
        if (convertResourceUrls) {
            return (converter == null) ? convertedFindResource(name) : findResource(name);            
        } else {
            return findResource(name);
        }
    }
    
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (convertResourceUrls) {
            return (converter == null) ? convertedFindResources(name) : findResources(name);   
        } else {
            return findResources(name);
        }
    }
          
    @Override
    protected URL convert(URL url) {
        try {
            URL convertedURL = converter.resolve(url);
            return convertedURL;
        } catch (IOException e) {
            e.printStackTrace();
            return url;
        }
    }
       
}
