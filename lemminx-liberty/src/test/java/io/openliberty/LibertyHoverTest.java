package io.openliberty;

import org.junit.jupiter.api.Test;

import static io.openliberty.tools.langserver.lemminx.LibertyXSDURIResolver.SERVER_XSD_RESOURCE;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import static org.eclipse.lemminx.XMLAssert.r;
import java.io.IOException;

public class LibertyHoverTest {

        static String newLine = System.lineSeparator();
        static String serverXMLURI = "test/server.xml";

        @Test
        public void testFeatureHover() throws BadLocationException {

                String serverXML = String.join(newLine, //
                                "<server description=\"Sample Liberty server\">", //
                                "       <featureManager>", //
                                "               <feature>j|axrs-2.1</feature>", //
                                "       </featureManager>", //
                                "</server>" //
                );

                XMLAssert.assertHover(serverXML, serverXMLURI,
                            "Description: This feature enables support for Java API for RESTful Web Services v2.1.  "
                                            + "JAX-RS annotations can be used to define web service clients and endpoints that comply with the REST architectural style. "
                                            + "Endpoints are accessed through a common interface that is based on the HTTP standard methods."
                                            + System.lineSeparator()
                                            + "Enabled by: microProfile-2.0, microProfile-2.1, microProfile-2.2, microProfile-3.0, microProfile-3.2, microProfile-3.3, microProfile-4.0, microProfile-4.1, mpOpenAPI-2.0, opentracing-1.3, opentracing-2.0, webProfile-8.0"
                                            + System.lineSeparator()
                                            + "Enables: jaxrsClient-2.1, servlet-4.0",
                            r(2, 24, 2, 33));

        }

        @Test
        public void testFeatureHoverTrim() throws BadLocationException {

                String serverXML = String.join(newLine, //
                                "<server description=\"Sample Liberty server\">", //
                                "       <featureManager>", //
                                "               <feature>j|axrs-2.1 </feature>", //
                                "       </featureManager>", //
                                "</server>" //
                );

                XMLAssert.assertHover(serverXML, serverXMLURI,
                                "Description: This feature enables support for Java API for RESTful Web Services v2.1.  "
                                                + "JAX-RS annotations can be used to define web service clients and endpoints that comply with the REST architectural style. "
                                                + "Endpoints are accessed through a common interface that is based on the HTTP standard methods."
                                                + System.lineSeparator()
                                                + "Enabled by: microProfile-2.0, microProfile-2.1, microProfile-2.2, microProfile-3.0, microProfile-3.2, microProfile-3.3, microProfile-4.0, microProfile-4.1, mpOpenAPI-2.0, opentracing-1.3, opentracing-2.0, webProfile-8.0"
                                                + System.lineSeparator()
                                                + "Enables: jaxrsClient-2.1, servlet-4.0",
                                r(2, 24, 2, 34));

        }

        @Test
        public void testXSDSchemaHover() throws BadLocationException, IOException {
                String serverXSDURI = SERVER_XSD_RESOURCE.getDeployedPath().toUri().toString().replace("///", "/");

                String serverXML = String.join(newLine, //
                                "<server description=\"Sample Liberty server\">", //
                                "       <feature|Manager>", //
                                "               <feature>jaxrs-2.1</feature>", //
                                "       </featureManager>", //
                                "</server>" //
                );

                XMLAssert.assertHover(serverXML, serverXMLURI, "Defines how the server loads features." + //
                                System.lineSeparator() + System.lineSeparator() + //
                                "Source: [server-cached-23.0.0.9.xsd](" + serverXSDURI + ")", //
                                r(1, 8, 1, 22));
        }

}
