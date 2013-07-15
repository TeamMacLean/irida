package ca.corefacility.bioinformatics.irida.web.controller.test.unit.samples;

import ca.corefacility.bioinformatics.irida.exceptions.InvalidPropertyException;
import ca.corefacility.bioinformatics.irida.model.Project;
import ca.corefacility.bioinformatics.irida.model.Relationship;
import ca.corefacility.bioinformatics.irida.model.Sample;
import ca.corefacility.bioinformatics.irida.model.SequenceFile;
import ca.corefacility.bioinformatics.irida.model.roles.impl.Identifier;
import ca.corefacility.bioinformatics.irida.service.ProjectService;
import ca.corefacility.bioinformatics.irida.service.RelationshipService;
import ca.corefacility.bioinformatics.irida.service.SampleService;
import ca.corefacility.bioinformatics.irida.service.SequenceFileService;
import ca.corefacility.bioinformatics.irida.web.assembler.resource.ResourceCollection;
import ca.corefacility.bioinformatics.irida.web.assembler.resource.RootResource;
import ca.corefacility.bioinformatics.irida.web.assembler.resource.sequencefile.SequenceFileResource;
import ca.corefacility.bioinformatics.irida.web.controller.api.GenericController;
import ca.corefacility.bioinformatics.irida.web.controller.api.projects.ProjectSequenceFilesController;
import ca.corefacility.bioinformatics.irida.web.controller.api.samples.SampleSequenceFilesController;
import ca.corefacility.bioinformatics.irida.web.controller.test.unit.TestDataFactory;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.net.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SampleSequenceFilesController}.
 */
public class SampleSequenceFilesControllerTest {
    private SampleSequenceFilesController controller;
    private SequenceFileService sequenceFileService;
    private SampleService sampleService;
    private RelationshipService relationshipService;
    private ProjectService projectService;

    @Before
    public void setUp() {
        sampleService = mock(SampleService.class);
        sequenceFileService = mock(SequenceFileService.class);
        relationshipService = mock(RelationshipService.class);
        projectService = mock(ProjectService.class);

        controller = new SampleSequenceFilesController(sequenceFileService, sampleService, relationshipService, projectService);
    }

    @Test
    public void testGetSampleSequenceFiles() throws IOException {
        Project p = TestDataFactory.constructProject();
        Sample s = TestDataFactory.constructSample();
        SequenceFile sf = TestDataFactory.constructSequenceFile();
        Relationship r = new Relationship();
        r.setIdentifier(new Identifier());
        r.setSubject(s.getIdentifier());
        r.setObject(sf.getIdentifier());
        Collection<Relationship> relationships = Sets.newHashSet(r);

        // mock out the service calls
        when(relationshipService.getRelationshipsForEntity(s.getIdentifier(), Sample.class, SequenceFile.class))
                .thenReturn(relationships);
        when(sequenceFileService.read(sf.getIdentifier())).thenReturn(sf);

        ModelMap modelMap = controller.getSampleSequenceFiles(p.getIdentifier().getIdentifier(), s.getIdentifier().getIdentifier());

        // verify that the service calls were used.
        verify(relationshipService, times(1)).getRelationshipsForEntity(s.getIdentifier(), Sample.class,
                SequenceFile.class);
        verify(sequenceFileService, times(1)).read(sf.getIdentifier());

        Object o = modelMap.get(GenericController.RESOURCE_NAME);
        assertTrue(o instanceof ResourceCollection);
        @SuppressWarnings("unchecked")
        ResourceCollection<SequenceFileResource> resources = (ResourceCollection<SequenceFileResource>) o;
        assertNotNull(resources);
        assertEquals(1, resources.size());

        Link selfCollection = resources.getLink(Link.REL_SELF);
        Link sample = resources.getLink(SampleSequenceFilesController.REL_SAMPLE);
        String sampleLocation = "http://localhost/projects/" + p.getIdentifier().getIdentifier() +
                "/samples/" + s.getIdentifier().getIdentifier();
        String sequenceFileLocation = sampleLocation + "/sequenceFiles/" + sf.getIdentifier().getIdentifier();

        assertEquals(sampleLocation + "/sequenceFiles", selfCollection.getHref());
        assertEquals(sampleLocation, sample.getHref());

        // confirm that the self rel for an individual sequence file exists
        SequenceFileResource sfr = resources.iterator().next();
        Link self = sfr.getLink(Link.REL_SELF);
        assertEquals(sequenceFileLocation, self.getHref());
        assertEquals(sf.getFile().toString(), sfr.getFile());

        // confirm that we have a link to the fasta formatted file
        Link fasta = sfr.getLink(SampleSequenceFilesController.REL_SAMPLE_SEQUENCE_FILE_FASTA);
        assertNotNull(fasta);
        assertEquals(sequenceFileLocation + ".fasta", fasta.getHref());
    }

    @Test
    public void testRemoveSequenceFileFromSample() throws IOException {
        Project p = TestDataFactory.constructProject();
        Sample s = TestDataFactory.constructSample();
        SequenceFile sf = TestDataFactory.constructSequenceFile();
        Relationship r = new Relationship();
        r.setSubject(p.getIdentifier());
        r.setObject(sf.getIdentifier());

        when(projectService.read(p.getIdentifier())).thenReturn(p);
        when(sampleService.read(s.getIdentifier())).thenReturn(s);
        when(sequenceFileService.read(sf.getIdentifier())).thenReturn(sf);
        when(sampleService.removeSequenceFileFromSample(p, s, sf)).thenReturn(r);

        ModelMap modelMap = controller.removeSequenceFileFromSample(p.getIdentifier().getIdentifier(),
                s.getIdentifier().getIdentifier(), sf.getIdentifier().getIdentifier());

        verify(projectService, times(1)).read(p.getIdentifier());
        verify(sampleService, times(1)).read(s.getIdentifier());
        verify(sequenceFileService, times(1)).read(sf.getIdentifier());

        verify(sampleService, times(1)).removeSequenceFileFromSample(p, s, sf);

        Object o = modelMap.get(GenericController.RESOURCE_NAME);
        assertNotNull(o);
        assertTrue(o instanceof RootResource);
        RootResource resource = (RootResource) o;

        Link sample = resource.getLink(SampleSequenceFilesController.REL_SAMPLE);
        Link sequenceFiles = resource.getLink(SampleSequenceFilesController.REL_SAMPLE_SEQUENCE_FILES);
        Link projectSequenceFile = resource.getLink(ProjectSequenceFilesController.REL_PROJECT_SEQUENCE_FILE);

        String projectLocation = "http://localhost/projects/" + p.getIdentifier().getIdentifier();
        String sampleLocation = projectLocation + "/samples/" + s.getIdentifier().getIdentifier();

        assertNotNull(sample);
        assertEquals(sampleLocation, sample.getHref());
        assertNotNull(sequenceFiles);
        assertEquals(sampleLocation + "/sequenceFiles", sequenceFiles.getHref());
        assertNotNull(projectSequenceFile);
        assertEquals(projectLocation + "/sequenceFiles/" + sf.getIdentifier().getIdentifier(), projectSequenceFile.getHref());
    }

    @Test
    public void testGetSequenceFileForSample() throws IOException {
        Project p = TestDataFactory.constructProject();
        Sample s = TestDataFactory.constructSample();
        SequenceFile sf = TestDataFactory.constructSequenceFile();

        when(projectService.read(p.getIdentifier())).thenReturn(p);
        when(sampleService.read(s.getIdentifier())).thenReturn(s);
        when(sequenceFileService.getSequenceFileFromSample(p, s, sf.getIdentifier())).thenReturn(sf);

        ModelMap modelMap = controller.getSequenceFileForSample(p.getIdentifier().getIdentifier(),
                s.getIdentifier().getIdentifier(), sf.getIdentifier().getIdentifier());

        verify(projectService).read(p.getIdentifier());
        verify(sampleService).read(s.getIdentifier());
        verify(sequenceFileService).getSequenceFileFromSample(p, s, sf.getIdentifier());

        Object o = modelMap.get(GenericController.RESOURCE_NAME);
        assertNotNull(o);
        assertTrue(o instanceof SequenceFileResource);
        SequenceFileResource sfr = (SequenceFileResource) o;
        assertEquals(sf.getFile().toString(), sfr.getFile());

        Link self = sfr.getLink(Link.REL_SELF);
        Link sampleSequenceFiles = sfr.getLink(SampleSequenceFilesController.REL_SAMPLE_SEQUENCE_FILES);
        Link sample = sfr.getLink(SampleSequenceFilesController.REL_SAMPLE);
        Link selfFasta = sfr.getLink(SampleSequenceFilesController.REL_SAMPLE_SEQUENCE_FILE_FASTA);

        String sampleLocation = "http://localhost/projects/" + p.getIdentifier().getIdentifier() +
                "/samples/" + s.getIdentifier().getIdentifier();
        String sequenceFileLocation = sampleLocation + "/sequenceFiles/" + sf.getIdentifier().getIdentifier();

        assertNotNull(self);
        assertEquals(sequenceFileLocation, self.getHref());
        assertNotNull(sampleSequenceFiles);
        assertEquals(sampleLocation + "/sequenceFiles", sampleSequenceFiles.getHref());
        assertNotNull(sample);
        assertEquals(sampleLocation, sample.getHref());
        assertNotNull(selfFasta);
        assertEquals(sequenceFileLocation + ".fasta", selfFasta.getHref());
    }

    @Test
    public void testAddNewSequenceFileToSample() throws IOException {
        Project p = TestDataFactory.constructProject();
        Sample s = TestDataFactory.constructSample();
        SequenceFile sf = TestDataFactory.constructSequenceFile();
        Relationship sampleSequenceFileRelationship = new Relationship(s.getIdentifier(), sf.getIdentifier());
        String projectId = p.getIdentifier().getIdentifier();
        String sampleId = s.getIdentifier().getIdentifier();
        String sequenceFileId = sf.getIdentifier().getIdentifier();

        Path f = Files.createTempFile(null, null);
        MockMultipartFile mmf = new MockMultipartFile("filename", "filename", "blurgh",
                FileCopyUtils.copyToByteArray(f.toFile()));

        when(sampleService.read(s.getIdentifier())).thenReturn(s);
        when(sequenceFileService.createSequenceFileWithOwner(any(SequenceFile.class), eq(Sample.class),
                eq(s.getIdentifier()))).thenReturn(sampleSequenceFileRelationship);
        when(projectService.read(p.getIdentifier())).thenReturn(p);

        ResponseEntity<String> response = controller.addNewSequenceFileToSample(p.getIdentifier().getIdentifier(),
                s.getIdentifier().getIdentifier(), mmf);

        verify(sampleService).getSampleForProject(p, s.getIdentifier());
        verify(projectService).read(p.getIdentifier());
        verify(sampleService, times(1)).read(s.getIdentifier());
        verify(sequenceFileService).createSequenceFileWithOwner(any(SequenceFile.class), eq(Sample.class),
                eq(s.getIdentifier()));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        List<String> locations = response.getHeaders().get(HttpHeaders.LOCATION);
        assertNotNull(locations);
        assertFalse(locations.isEmpty());
        assertEquals(1, locations.size());
        assertEquals("http://localhost/projects/" + projectId + "/samples/" + sampleId
                + "/sequenceFiles/" + sequenceFileId, locations.iterator().next());

        Files.delete(f);
    }

    @Test
    public void testAddExistingSequenceFileToSample() throws IOException {
        Project p = TestDataFactory.constructProject();
        Sample s = TestDataFactory.constructSample();
        SequenceFile sf = TestDataFactory.constructSequenceFile();
        Relationship sampleSequenceFileRelationship = new Relationship(s.getIdentifier(), sf.getIdentifier());
        String projectId = p.getIdentifier().getIdentifier();
        String sampleId = s.getIdentifier().getIdentifier();
        String sequenceFileId = sf.getIdentifier().getIdentifier();

        when(projectService.read(p.getIdentifier())).thenReturn(p);
        when(sampleService.getSampleForProject(p, s.getIdentifier())).thenReturn(s);
        when(sequenceFileService.read(sf.getIdentifier())).thenReturn(sf);
        when(sampleService.addSequenceFileToSample(p, s, sf)).thenReturn(sampleSequenceFileRelationship);

        Map<String, String> requestBody = ImmutableMap.of(SampleSequenceFilesController.SEQUENCE_FILE_ID_KEY, sequenceFileId);

        ResponseEntity<String> response = controller.addExistingSequenceFileToSample(projectId, sampleId, requestBody);

        verify(projectService).read(p.getIdentifier());
        verify(sampleService).getSampleForProject(p, s.getIdentifier());
        verify(sequenceFileService).read(sf.getIdentifier());
        verify(sampleService).addSequenceFileToSample(p, s, sf);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        List<String> locations = response.getHeaders().get(HttpHeaders.LOCATION);
        assertNotNull(locations);
        assertFalse(locations.isEmpty());
        assertEquals(1, locations.size());
        // the sequence file location is still the same, but we've added a new relationship
        assertEquals("http://localhost/projects/" + projectId +
                "/samples/" + sampleId +
                "/sequenceFiles/" + sequenceFileId, locations.iterator().next());
    }

    @Test
    public void testAddExistingSequenceFileToSampleBadRequest() {
        Map<String, String> requestBody = new HashMap<>();
        try {
            controller.addExistingSequenceFileToSample(UUID.randomUUID().toString(), UUID.randomUUID().toString(), requestBody);
            fail();
        } catch (InvalidPropertyException e) {

        } catch (Exception e) {
            fail();
        }
    }
}
