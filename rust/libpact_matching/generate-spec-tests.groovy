#!/usr/bin/env groovy

import groovy.io.FileType
import groovy.json.JsonSlurper

def tests = new File('tests')
def specs = new File(tests, 'spec_testcases')
specs.eachFileRecurse(FileType.DIRECTORIES) { dir ->
  def path = dir.toPath()
  def testFile = new File(dir, 'mod.rs')
  def requestResponsePath = path.getNameCount() > 3 ? path.getName(3).toString() : ''
  def specVersion = path.getName(2).toString().toUpperCase()

  testFile.withPrintWriter { pw ->
    pw.println('#[allow(unused_imports)]')
    pw.println('use env_logger;')
    pw.println('#[allow(unused_imports)]')
    pw.println('use pact_matching::models::PactSpecification;')
    if (requestResponsePath == 'request') {
      pw.println('#[allow(unused_imports)]')
      pw.println('use pact_matching::models::Request;')
      pw.println('#[allow(unused_imports)]')
      pw.println('use pact_matching::match_request;')
      pw.println('#[allow(unused_imports)]')
      pw.println('use rustc_serialize::json::Json;')
      pw.println('#[allow(unused_imports)]')
      pw.println('use expectest::prelude::*;')
    } else if (requestResponsePath == 'response') {
      pw.println('#[allow(unused_imports)]')
      pw.println('use pact_matching::models::Response;')
      pw.println('#[allow(unused_imports)]')
      pw.println('use pact_matching::match_response;')
      pw.println('#[allow(unused_imports)]')
      pw.println('use rustc_serialize::json::Json;')
      pw.println('#[allow(unused_imports)]')
      pw.println('use expectest::prelude::*;')
    } else if (requestResponsePath == 'message') {
      pw.println('#[allow(unused_imports)]')
      pw.println('use pact_matching::models::message::Message;')
      pw.println('#[allow(unused_imports)]')
      pw.println('use pact_matching::match_message;')
      pw.println('#[allow(unused_imports)]')
      pw.println('use rustc_serialize::json::Json;')
      pw.println('#[allow(unused_imports)]')
      pw.println('use expectest::prelude::*;')
    }

    dir.eachDir {
      pw.println "mod $it.name;"
    }

    dir.eachFileMatch(~/.*\.json/) {
      def json = new JsonSlurper().parse(it)
      def testBody = """
        |#[test]
        |fn ${it.name.replaceAll(' ', '_').replaceAll('-', '_').replaceAll('\\.json', '')}() {
        |    env_logger::init().unwrap_or(());
        |    let pact = Json::from_str(r#"
      """
      it.text.eachLine { line ->
        testBody += '|      ' + line + '\n'
      }
      testBody += '|    "#).unwrap();' + '\n'
      if (requestResponsePath == 'request') {
        testBody += """
        |    let expected = Request::from_json(&pact.find("expected").unwrap(), &PactSpecification::$specVersion);
        |    println!("{:?}", expected);
        |    let actual = Request::from_json(&pact.find("actual").unwrap(), &PactSpecification::$specVersion);
        |    println!("{:?}", actual);
        |    let pact_match = pact.find("match").unwrap();
        |    if pact_match.as_boolean().unwrap() {
        |       expect!(match_request(expected, actual)).to(be_empty());
        |    } else {
        |       expect!(match_request(expected, actual)).to_not(be_empty());
        |    }
        """
      } else if (requestResponsePath == 'response') {
        testBody += """
        |    let expected = Response::from_json(&pact.find("expected").unwrap(), &PactSpecification::$specVersion);
        |    println!("{:?}", expected);
        |    let actual = Response::from_json(&pact.find("actual").unwrap(), &PactSpecification::$specVersion);
        |    println!("{:?}", actual);
        |    let pact_match = pact.find("match").unwrap();
        |    let result = match_response(expected, actual);
        |    if pact_match.as_boolean().unwrap() {
        |       expect!(result).to(be_empty());
        |    } else {
        |       expect!(result).to_not(be_empty());
        |    }
        """
      } else if (requestResponsePath == 'message') {
        testBody += """
        |    let expected = Message::from_json(0, &pact.find("expected").unwrap(), &PactSpecification::$specVersion).unwrap();
        |    println!("{:?}", expected);
        |    let actual = Message::from_json(0, &pact.find("actual").unwrap(), &PactSpecification::$specVersion).unwrap();
        |    println!("{:?}", actual);
        |    let pact_match = pact.find("match").unwrap();
        |    let result = match_message(expected, actual);
        |    if pact_match.as_boolean().unwrap() {
        |       expect!(result).to(be_empty());
        |    } else {
        |       expect!(result).to_not(be_empty());
        |    }
        """
      }
      testBody += '|}'
      pw.println testBody.stripMargin('|')
    }
  }
}
