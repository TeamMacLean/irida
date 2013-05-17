/*
 * Copyright 2013 Thomas Matthews <thomas.matthews@phac-aspc.gc.ca>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.corefacility.bioinformatics.irida.repositories.sesame;

import ca.corefacility.bioinformatics.irida.repositories.sesame.dao.SailMemoryStore;
import ca.corefacility.bioinformatics.irida.exceptions.EntityNotFoundException;
import ca.corefacility.bioinformatics.irida.model.enums.Order;
import ca.corefacility.bioinformatics.irida.model.roles.impl.Identifier;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 *
 * @author Thomas Matthews <thomas.matthews@phac-aspc.gc.ca>
 */
public class GenericRepositoryTest {
    
    IdentifiedRepo repo;
    
    public GenericRepositoryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }
    
    @Before
    public void setUp() throws NoSuchMethodException {
        SailMemoryStore store = new SailMemoryStore();
        store.initialize();
        AuditRepository auditRepo = new AuditRepository(store);
        RelationshipSesameRepository linksRepo = new RelationshipSesameRepository(store, auditRepo);
        
        repo = new IdentifiedRepo(store,auditRepo,linksRepo);
        
        repo.create(new Identified("data1"));
        repo.create(new Identified("data2"));
        repo.create(new Identified("data3"));
    }
    
    /**
     * Test of generateIdentifier method, of class GenericRepository.
     
    @Test
    public void testGenerateIdentifier() {
        Identified i = new Identified("blah");
        Identifier id = repo.generateNewIdentifier(i);
        assertNotNull(id.getIdentifier());
    }*/

    /**
     * Test of create method, of class GenericRepository.
     */
    @Test
    public void testCreate() {
        Identified i = new Identified("newdata");

        try {
            i = repo.create(i);
        } catch (IllegalArgumentException e) {
            fail();
        }
    }
    
    @Test
    public void testAddInvalidObject() {
        Identified i = null;
        
        try {
            repo.create(i);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }    

    /**
     * Test of read method, of class GenericRepository.
     */
    @Test
    public void testRead() {
        Identified i = new Identified("newdata");

        i = repo.create(i);
        try{
            i = repo.read(i.getIdentifier());
            assertNotNull(i);
        }
        catch(IllegalArgumentException e){
            System.err.println(e.getMessage());
            fail();
        }
    }
    
    @Test
    public void testReadInvalid() {
        try{
            Identifier i = new Identifier();
            i.setUri(URI.create("http://nowhere/fake"));
            Identified u = repo.read(i);
            fail();
        }
        catch(EntityNotFoundException e){
            assertNotNull(e);
        }
    } 
    
    /**
     * Test of list method, of class GenericRepository.
     */
    @Test
    public void testList_0args() {
        List<Identified> users = repo.list();
        if(users.isEmpty()){
            fail();
        }
    }

    /**
     * Test of list method, of class GenericRepository.
     */
    @Test
    public void testList_4args() {
        List<Identified> users = repo.list(0, 1, null, Order.ASCENDING);
        
        if(users.size() != 1){
            fail();
        }
        
        users = repo.list(0, 2, null, Order.ASCENDING);
        if(users.size() != 2){
            fail();
        }
    }

    /**
     * Test of delete method, of class GenericRepository.
     */
    @Test
    public void testDelete() {
        Identified u = new Identified("newdata");
        u = repo.create(u);
        
        try{
            repo.delete(u.getIdentifier());
            if(repo.exists(u.getIdentifier())){
                fail();
            }
        }
        catch(IllegalArgumentException e){
            fail();
        }
    }
    
    @Test
    public void testDeleteInvalid() {
        Identifier i = new Identifier();
        i.setUri(URI.create("http://nowhere/fake"));
        
        try{
            repo.delete(i);
            fail();
        }
        catch(EntityNotFoundException e){}
    }    
    /**
     * Test of exists method, of class GenericRepository.
     */
    @Test
    public void testExists() {
        Identified u = new Identified("newdata");
        u = repo.create(u);
        
        try{
            if(! repo.exists(u.getIdentifier())){
                fail();
            }
        }
        catch(IllegalArgumentException e){
            fail();
        } 
    }

    /**
     * Test of update method, of class GenericRepository.
     */
    @Test
    public void testUpdate() {
        Identified u = new Identified("newdata");
        u = repo.create(u);
        
        try{
            u.setData("different");
            u = repo.update(u);
            
            Identified j = repo.read(u.getIdentifier());
            assertNotNull(j);
            assertTrue(j.getData().compareTo(u.getData())==0);
        }
        catch(IllegalArgumentException e){
            fail();
        }
    }
    
    /**
     * Test update method with 2 params
     */
    @Test
    public void testUpdate_2param() {
        Identified u = new Identified("newdata");
        u = repo.create(u);
        //public Type update(IDType id, Map<String, Object> updatedFields) throws InvalidPropertyException,SecurityException {        
        
        try{
            String differentData = "different";
            HashMap<String,Object> changes = new HashMap<>();
            changes.put("data", differentData);
            u = repo.update(u.getIdentifier(), changes);
            
            Identified j = repo.read(u.getIdentifier());
            assertNotNull(j);
            assertTrue(j.getData().compareTo(differentData)==0);
        }
        catch(IllegalArgumentException e){
            fail();
        }
    }    

    
    /**
     * Test of count method, of class GenericRepository.
     */
    @Test
    public void testCount() {
        repo.create(new Identified("newdata"));
        
        assertTrue(repo.count()> 0);
    }

    /**
     * Test of generateNewIdentifier method, of class GenericRepository.
     
    @Test
    public void testGenerateNewIdentifier() {
        Identified i = new Identified();
        i.setData("blah");

        Identifier result = repo.generateNewIdentifier(i);
        assertNotNull(result);
    }*/

    /**
     * Test of buildIdentifier method, of class GenericRepository.
     
    @Test
    public void testBuildIdentifier() {
        System.out.println("buildIdentifier");
        Object obj = null;
        String identifiedBy = "";
        GenericRepository instance = new GenericRepositoryImpl();
        Identifier expResult = null;
        Identifier result = instance.buildIdentifier(obj, identifiedBy);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/

    /**
     * Test of buildObject method, of class GenericRepository.
     
    @Test
    public void testBuildObject() {
        System.out.println("buildObject");
        Object base = null;
        Identifier i = null;
        GenericRepository instance = new GenericRepositoryImpl();
        Object expResult = null;
        Object result = instance.buildObject(base, i);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/

    /**
     * Test of storeObject method, of class GenericRepository.
     
    @Test
    public void testStoreObject() {
        System.out.println("storeObject");
        Object object = null;
        GenericRepository instance = new GenericRepositoryImpl();
        Object expResult = null;
        Object result = instance.storeObject(object);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/

    /**
     * Test of buildObjectFromResult method, of class GenericRepository.
     
    @Test
    public void testBuildObjectFromResult() throws Exception {
        System.out.println("buildObjectFromResult");
        Object o = null;
        org.openrdf.model.URI u = null;
        ObjectConnection con = null;
        GenericRepository instance = new GenericRepositoryImpl();
        Object expResult = null;
        Object result = instance.buildObjectFromResult(o, u, con);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/

    /**
     * Test of readMultiple method, of class GenericRepository.
     
    @Test
    public void testReadMultiple() {
        System.out.println("readMultiple");
        List<Identifier> idents = null;
        GenericRepository instance = new GenericRepositoryImpl();
        List expResult = null;
        List result = instance.readMultiple(idents);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }*/
    
    

}