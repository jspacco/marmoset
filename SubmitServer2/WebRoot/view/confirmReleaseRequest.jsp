<%--

 Marmoset: a student project snapshot, submission, testing and code review
 system developed by the Univ. of Maryland, College Park
 
 Developed as part of Jaime Spacco's Ph.D. thesis work, continuing effort led
 by William Pugh. See http://marmoset.cs.umd.edu/
 
 Copyright 2005 - 2011, Univ. of Maryland
 
 Licensed under the Apache License, Version 2.0 (the "License"); you may not
 use this file except in compliance with the License. You may obtain a copy of
 the License at
 
 http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 License for the specific language governing permissions and limitations under
 the License.

--%>


<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="ss" uri="http://www.cs.umd.edu/marmoset/ss"%>

<!DOCTYPE HTML>
<html>
<ss:head
	title="Confirm release request for submission ${ss:scrub(param.submissionPK)}" />

<body>
<ss:header />
<ss:breadCrumb />

 <ss:releaseTokens/>

<h2>Are you sure you want to release test submission
${ss:scrub(param.submissionPK)}?</h2>

<p>Click OK to release test this submission or the back button on your
browser to cancel.</p>

<c:url var="releaseRequestLink" value="/action/RequestReleaseTest" />

<form method="POST" action="${releaseRequestLink}"><input type="hidden"
	name="submissionPK" value="${ss:scrub(param.submissionPK)}"> 
	<p><input type="submit"	value="OK"></p>
</form>

<ss:footer />
</body>
</html>
