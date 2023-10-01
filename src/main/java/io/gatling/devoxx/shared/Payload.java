package io.gatling.devoxx.shared;

public final class Payload {

  public static final String JSON_1K = """
    {"flavors":[
      {"id":"1","links":[{"href":"http://helloworld.example.com/v2/helloworld/flavors/1","rel":"self"},{"href":"http://helloworld.example.com/helloworld/flavors/1","rel":"bookmark"}],"name":"m1.tiny"},
      {"id":"2","links":[{"href":"http://helloworld.example.com/v2/helloworld/flavors/2","rel":"self"},{"href":"http://helloworld.example.com/helloworld/flavors/2","rel":"bookmark"}],"name":"m1.small"},
      {"id":"3","links":[{"href":"http://helloworld.example.com/v2/helloworld/flavors/3","rel":"self"},{"href":"http://helloworld.example.com/helloworld/flavors/3","rel":"bookmark"}],"name":"m1.medium"},
      {"id":"4","links":[{"href":"http://helloworld.example.com/v2/helloworld/flavors/4","rel":"self"},{"href":"http://helloworld.example.com/helloworld/flavors/4","rel":"bookmark"}],"name":"m1.large"},
      {"id":"5","links":[{"href":"http://helloworld.example.com/v2/helloworld/flavors/5","rel":"self"},{"href":"http://helloworld.example.com/helloworld/flavors/5","rel":"bookmark"}],"name":"m1.xlarge"}]
    }
    """.stripIndent();

  private Payload() {
  }
}
