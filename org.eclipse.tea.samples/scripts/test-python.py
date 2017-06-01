# this is a comment
# name : Run the Task
# toolbar : Project Explorer
# image : platform:/plugin/org.eclipse.tea.core/resources/tea.png

from org.eclipse.core.runtime import IStatus, Status;

loadModule("/TEA/Tasking");

def task():
	getLog().log('this is a python log');
	getContext().set(IStatus, Status.CANCEL_STATUS);
	
runTaskChain(createTaskChain("My Test Chain", ['task()']));
runTaskChain(lookupTaskChain("Something"));
