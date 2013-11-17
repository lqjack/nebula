/*******************************************************************************
 * Copyright (c) 2008, 2010 VMware Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   VMware Inc. - initial contribution
 *******************************************************************************/

package org.eclipse.virgo.kernel.userregion.internal.equinox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import org.eclipse.virgo.kernel.osgi.framework.BundleClassLoaderUnavailableException;
import org.eclipse.virgo.kernel.osgi.framework.InstrumentableClassLoader;
import org.eclipse.virgo.kernel.userregion.internal.equinox.EquinoxOsgiFramework;
import org.eclipse.virgo.kernel.userregion.internal.equinox.KernelBundleClassLoader;

/**
 */
public class EquinoxOsgiFrameworkTests extends AbstractOsgiFrameworkLaunchingTests {
    
    @Override
    protected String getRepositoryConfigDirectory() {
        return new File("src/test/resources/config/EquinoxOsgiFrameworkTests").getAbsolutePath();
    }

    @Test
    public void testStartAndStop() throws Exception {
        assertNotNull(this.framework.getBundleContext());
        assertEquals(Bundle.ACTIVE, this.framework.getBundleContext().getBundle().getState());
    }

    @Test
    public void testGetClassBundle() throws Exception {
        Class<?> c = this.framework.getBundleContext().getBundle().loadClass("org.osgi.framework.Bundle");
        assertNotNull(c);
        Bundle b = this.framework.getClassBundle(c);
        assertNotNull(b);
        assertEquals(0, b.getBundleId());
    }

    @Test(expected = BundleClassLoaderUnavailableException.class)    
    public void testGetClassLoaderFromUnresolved() throws Exception {
        Bundle faultyBundle = this.framework.getBundleContext().installBundle(new File("src/test/resources/EquinoxOsgiFrameworkTests/faulty").toURI().toString());
        assertEquals(Bundle.INSTALLED, faultyBundle.getState());
        this.framework.getBundleClassLoader(faultyBundle);
    }

    @Test
    public void testLoadClassAndGetClassLoader() throws Exception {
        Bundle bundle = installSpringCore(this.framework);
        assertEquals("incorrect bundle loaded", "org.springframework.core", bundle.getSymbolicName());
        Class<?> cls = bundle.loadClass("org.springframework.core.JdkVersion");
        assertNotNull(cls);
        assertTrue(cls.getClassLoader() instanceof KernelBundleClassLoader);
        assertTrue("classloader is screwed", cls.getClassLoader().toString().contains("org.springframework.core"));
    }

    @Test
    public void testAddClassFileTransformer() throws Exception {
        Bundle bundle = installSpringCore(this.framework);
        ClassLoader bundleClassLoader = this.framework.getBundleClassLoader(bundle);
        assertNotNull(bundleClassLoader);
        InstrumentableClassLoader icl = (InstrumentableClassLoader) bundleClassLoader;
        final AtomicInteger count = new AtomicInteger(0);
        icl.addClassFileTransformer(new ClassFileTransformer() {

            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                byte[] classfileBuffer) throws IllegalClassFormatException {
                count.incrementAndGet();
                return null;
            }

        });
        bundle.loadClass("org.springframework.core.JdkVersion");
        assertEquals(1, count.get());
    }

    /**
     * @param osgi
     * @return
     * @throws BundleException
     */
    private Bundle installSpringCore(EquinoxOsgiFramework osgi) throws BundleException {
        osgi.getBundleContext().installBundle("file:///" + new File(System.getProperty("user.home") + "/virgo-build-cache/ivy-cache/repository/org.apache.commons/com.springsource.org.apache.commons.logging/1.1.1/com.springsource.org.apache.commons.logging-1.1.1.jar").getAbsolutePath());
        return osgi.getBundleContext().installBundle("file:///" + new File(System.getProperty("user.home") + "/virgo-build-cache/ivy-cache/repository/org.springframework/org.springframework.core/2.5.6.SEC01/org.springframework.core-2.5.6.SEC01.jar").getAbsolutePath());
    }
}
