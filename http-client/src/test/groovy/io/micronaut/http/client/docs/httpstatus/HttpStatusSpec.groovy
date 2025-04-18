/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.http.client.docs.httpstatus

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.server.EmbeddedServer
import spock.lang.*

class HttpStatusSpec extends Specification {

    @Shared
    @AutoCleanup
    EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer,
            ["spec.name": "httpstatus"],
            Environment.TEST)

    @AutoCleanup
    @Shared
    HttpClient client = embeddedServer.applicationContext.createBean(HttpClient, embeddedServer.getURL())

    @Unroll("#description")
    void "verify default HTTP status and @Status override"() {
        when:
        HttpResponse<String> response = client.toBlocking().exchange(HttpRequest.GET(uri), String)
        Optional<String> body = response.getBody()

        then:
        response.status == status
        body.isPresent()
        body.get() == 'success'

        where:
        uri                          | status              | description
        "/status/simple"             | HttpStatus.OK       | '200 status is returned by default'
        "/status"                    | HttpStatus.CREATED  | 'It is possible to control the status with @Status'
        "/httpResponseStatus"        | HttpStatus.CREATED  | 'You can specify status with HttpResponse.status'
    }

    @Issue("https://github.com/micronaut-projects/micronaut-core/issues/866")
    @Unroll
    void "Verify a controller can be annotated with @Status and void return for #uri"() {
        when:
        HttpResponse response = client.toBlocking().exchange(HttpRequest.GET(uri))

        then:
        response.status == status

        where:
        uri                          | status
        "/status/voidreturn"         | HttpStatus.CREATED
        "/status/completableVoid"    | HttpStatus.CREATED
        "/status/maybeVoid"          | HttpStatus.CREATED
    }

    void "Verify a controller can return HttpStatus"() {
        given:
        String uri = "/respondHttpStatus"
        when:
        HttpResponse response = client.toBlocking().exchange(HttpRequest.GET(uri))

        then:
        response.status == HttpStatus.CREATED
    }

    void "Simple custom return HttpStatus 404"() {
        given:
        String uri = "/status/simple404"
        when:
        client.toBlocking().exchange(HttpRequest.GET(uri), String)

        then:
        HttpClientResponseException e = thrown()
        e.message == "success"
        e.status == HttpStatus.NOT_FOUND
        HttpStatus.NOT_FOUND.code == e.code()
        HttpStatus.NOT_FOUND.reason == e.reason()
    }

    @Issue("https://github.com/micronaut-projects/micronaut-core/issues/5348")
    void "test returning a reactive stream of httpstatus"() {
        when:
        HttpResponse response = client.toBlocking().exchange(HttpRequest.GET("/status/reactive"))

        then:
        response.status() == HttpStatus.CREATED

        when:
        response = client.toBlocking().exchange(HttpRequest.GET("/status/single"))

        then:
        response.status() == HttpStatus.CREATED
    }
}
