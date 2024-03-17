package freemind.controller.actions.generated.instance;
/* HookNodeAction...*/
import java.util.ArrayList;
public class HookNodeAction extends NodeAction {
  protected String hookName;
  public String getHookName(){
    return hookName;
  }
  public void setHookName(String value){
    this.hookName = value;
  }
  public void addNodeListMember(NodeListMember nodeListMember) {
    nodeListMemberList.add(nodeListMember);
  }

  public void addAtNodeListMember(int position, NodeListMember nodeListMember) {
    nodeListMemberList.add(position, nodeListMember);
  }

  public NodeListMember getNodeListMember(int index) {
    return (NodeListMember)nodeListMemberList.get( index );
  }

  public int sizeNodeListMemberList() {
    return nodeListMemberList.size();
  }

  public void clearNodeListMemberList() {
    nodeListMemberList.clear();
  }

  public java.util.List getListNodeListMemberList() {
    return java.util.Collections.unmodifiableList(nodeListMemberList);
  }
    protected ArrayList nodeListMemberList = new ArrayList();

} /* HookNodeAction*/
