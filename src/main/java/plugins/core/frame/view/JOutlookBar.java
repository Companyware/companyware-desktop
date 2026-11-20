/*
 * Copyright 2015 Torridity.
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
 *
 * Modified for Companyware.
 * Modifications Copyright 2023-2026 mbdus-Softwareentwicklung - Mathias Bauer.
 */
package plugins.core.frame.view;


/*

  @author Torridity
 */
// Import the GUI classes
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.*;

import core.ApplicationContextProvider;
import core.TextMessages;
import core.ui.CompanywareTheme;
import platform.api.ui.UiContributionSpec;
import platform.api.ui.CompanywareIcons;

// Import the Java classes

/**
 * A JOutlookBar provides a component that is similar to a JTabbedPane, but instead of maintaining
 * tabs, it uses Outlook-style bars to control the visible component
 */
public class JOutlookBar extends JPanel implements ActionListener {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
     * The top panel: contains the buttons displayed on the top of the JOutlookBar
     */
    private JPanel topPanel = new JPanel();
    /**
     * The bottom panel: contains the buttons displayed on the bottom of the JOutlookBar
     */
    private JPanel bottomPanel = new JPanel(new GridLayout(1, 1));
    private JPanel southPanel = new JPanel(new BorderLayout());
    private JButton footerButton;
    private JButton logoutFooterButton;
    private final JPanel footerActions = new JPanel();
    private String selectedEntry;
    /**
     * A LinkedHashMap of bars: we use a linked hash map to preserve the order of the bars
     */
    private Map bars = new LinkedHashMap();
    private final Set<String> sdkBars = new HashSet<String>();
    private JButton activeNavigationChild;
    private JComponent currentSdkView;
    private final Map<JButton, JComponent> navigationChildTargets = new LinkedHashMap<JButton, JComponent>();
    
    private Map barEntries = new LinkedHashMap();
    
    private Map barIconEntries = new LinkedHashMap();
    /**
     * The currently visible bar (zero-based index)
     */
    private int visibleBar = -1;
    
    /**
     * A place-holder for the currently visible component
     */
    private JComponent visibleComponent = null;

    /**
     * Creates a new JOutlookBar; after which you should make repeated calls to
     * addBar() for each bar
     */
    public JOutlookBar() {
        this.setLayout(new BorderLayout());
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		this.setBackground(CompanywareTheme.sidebar());
		topPanel.setBackground(CompanywareTheme.sidebar());
		bottomPanel.setBackground(CompanywareTheme.sidebar());
		southPanel.setBackground(CompanywareTheme.sidebar());
		footerActions.setLayout(new BoxLayout(footerActions, BoxLayout.Y_AXIS));
		footerActions.setBackground(CompanywareTheme.sidebar());
		this.setBorder(BorderFactory.createEmptyBorder(8, 10, 14, 10));
        this.add(topPanel, BorderLayout.NORTH);
        southPanel.add(bottomPanel, BorderLayout.CENTER);
        southPanel.add(footerActions, BorderLayout.SOUTH);
        this.add(southPanel, BorderLayout.SOUTH);
    }
    
    public void setPreferredSize(){
    	int height = this.getHeight();
    	height = this.getBarEntrySize()*120;
    	this.setPreferredSize(new Dimension(196,height));
    }
    
    public void addBarEntry(String name) {
        Map entries = new LinkedHashMap();
        Map labels = (Map) this.barEntries.get(name);
        if(labels ==null){
        	labels = new LinkedHashMap();
        }
        labels.put(name, entries);
        this.barEntries.put(name, labels);
    }

    /**
     * Adds the specified component to the JOutlookBar and sets the bar's name
     * 
     * @param  name      The name of the outlook bar
     * @param  component   The component to add to the bar
     */
    public void addBar(String name, JComponent component) {
        BarInfo barInfo = new BarInfo(name, component);
        barInfo.getButton().addActionListener(this);
        this.bars.put(name, barInfo);
        render();
    }
    
    /**
     * Adds the specified component to the JOutlookBar and sets the bar's name
     * 
     * @param  name      The name of the outlook bar
     * @param  component   The component to add to the bar
     */
    public void addBar(String name, JComponent component, int priority) {
        BarInfo barInfo = new BarInfo(name, component, priority);
        barInfo.getButton().addActionListener(this);
        this.bars.put(name, barInfo);
        render();
    }

    /**
     * Adds the specified component to the JOutlookBar and sets the bar's name
     * 
     * @param  name      The name of the outlook bar
     * @param  icon      An icon to display in the outlook bar
     * @param  component   The component to add to the bar
     */
    public void addBar(String name, Icon icon, JComponent component) {
        BarInfo barInfo = new BarInfo(name, icon, component);
        barInfo.getButton().addActionListener(this);
        this.bars.put(name, barInfo);
        render();
    }
    
    public void addBarEntryIcon(String entryName, String iconName, Icon image, Object controller) {
    	if(this.barEntries == null){
    		this.barEntries = new LinkedHashMap();
    	}
    	Map labels = (Map) this.barEntries.get(entryName);
    	boolean exists = false;
    	
	    if(labels.containsKey(entryName)){
	    	this.barIconEntries = (LinkedHashMap) labels.get(entryName);
	    	if(this.barIconEntries.containsKey(iconName)){
	    		exists = true;
	    	}
	    }
    	
    	if(!exists){
    		JLabel label = new JLabel();
    		TextMessages service = ApplicationContextProvider.getContext().getBean(TextMessages.class);
    		label.setText(service.get(iconName));
    		label.setName(iconName);
    		if(image == null){
    			label.setVerticalTextPosition(JLabel.CENTER);
    		}
    		else{
    			label.setIcon(image);
    			label.setVerticalTextPosition(JLabel.BOTTOM);
    		}
			label.setHorizontalAlignment(JLabel.CENTER);
			label.setHorizontalTextPosition(JLabel.CENTER);
			label.setForeground(CompanywareTheme.text());
			label.setFont(CompanywareTheme.BODY);
			label.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));
			Dimension entrySize = new Dimension(176, image == null ? 42 : 72);
			label.setPreferredSize(entrySize);
			label.setMinimumSize(entrySize);
			label.setMaximumSize(entrySize);
    		if(this.barIconEntries == null){
    			this.barIconEntries = new LinkedHashMap();
    		}
    		label.addMouseListener((MouseListener) controller);
    		this.barIconEntries.put(iconName, label);
    		labels.put(entryName, this.barIconEntries);
    	}
    }

	public void replaceSdkNavigation(List<UiContributionSpec> navigation,
            List<UiContributionSpec> views, Consumer<JComponent> viewConsumer) {
		activeNavigationChild = null;
		currentSdkView = null;
		navigationChildTargets.clear();
		for (String id : new HashSet<String>(sdkBars)) bars.remove(id);
		sdkBars.clear();
		PluginNavigationGroup group = null;
		for (UiContributionSpec item : navigation) {
			UiContributionSpec target = findView(item, views);
			String id = "sdk:" + item.owner().value() + ":" + item.id();
			if (item.target() == null) {
				JPanel navigationPanel = navigationGroupPanel();
				BarInfo barInfo = new BarInfo(item.title(), navigationPanel, item.order(), null, false, true);
				barInfo.setTitleSupplier(() -> item.title());
				barInfo.getButton().addActionListener(this);
				bars.put(id, barInfo);
				sdkBars.add(id);
				group = new PluginNavigationGroup(item.owner().value(), navigationPanel);
				continue;
			}
			if (target == null) continue;
			if (group != null && group.owner.equals(item.owner().value())) {
				group.panel.add(navigationChildButton(item, target, viewConsumer));
				continue;
			}
			group = null;
			JPanel navigationPanel = new JPanel();
			navigationPanel.setBackground(CompanywareTheme.sidebar());
			BarInfo barInfo = new BarInfo(item.title(), navigationPanel, item.order(),
					() -> viewConsumer.accept(target.component()), false, false);
			barInfo.setTitleSupplier(() -> item.title());
			barInfo.getButton().addActionListener(this);
			bars.put(id, barInfo);
			sdkBars.add(id);
		}
		render();
    }

	private JPanel navigationGroupPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(CompanywareTheme.sidebar());
		panel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
		return panel;
	}

	private JButton navigationChildButton(UiContributionSpec item, UiContributionSpec target,
			Consumer<JComponent> viewConsumer) {
		final JComponent targetComponent = target.component();
		JButton button = new JButton(item.title());
		CompanywareTheme.navigationButton(button);
		button.setIcon(item.icon() == null ? CompanywareIcons.DOCUMENT : item.icon());
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		button.setIconTextGap(5);
		button.setAlignmentX(Component.LEFT_ALIGNMENT);
		int height = 72;
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		button.setPreferredSize(new Dimension(176, height));
		button.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.setRolloverEnabled(true);
		button.setFocusPainted(true);
		navigationChildTargets.put(button, targetComponent);
		button.addActionListener(event -> {
			activeNavigationChild = button;
			refreshActiveNavigationChild();
			viewConsumer.accept(targetComponent);
		});
		return button;
	}

	public void selectSdkView(JComponent component) {
		setSelectedEntry("");
		currentSdkView = component;
		activeNavigationChild = null;
		for (Map.Entry<JButton, JComponent> entry : navigationChildTargets.entrySet()) {
			if (entry.getValue() == component) {
				activeNavigationChild = entry.getKey();
				break;
			}
		}
		refreshActiveNavigationChild();
	}

	private void refreshActiveNavigationChild() {
		BarInfo visibleHeading = null;
		int visibleIndex = 0;
		for (Object value : bars.values()) {
			BarInfo info = (BarInfo) value;
			if (visibleIndex++ == visibleBar && info.isHeading()) visibleHeading = info;
		}
		for (String id : sdkBars) {
			BarInfo info = (BarInfo) bars.get(id);
			if (info == null || !info.isHeading()) continue;
			for (Component child : info.getComponent().getComponents()) {
				if (!(child instanceof JButton)) continue;
				JButton button = (JButton) child;
				boolean active = info == visibleHeading
						&& navigationChildTargets.get(button) == currentSdkView;
				button.putClientProperty("companyware.navigation.activeChild", Boolean.valueOf(active));
				button.setBackground(active ? CompanywareTheme.navigationSelected() : CompanywareTheme.sidebar());
				button.setBorder(active ? CompanywareTheme.selectedNavigationBorder()
						: BorderFactory.createEmptyBorder(6, 8, 6, 8));
			}
		}
	}

	private static final class PluginNavigationGroup {
		private final String owner;
		private final JPanel panel;

		private PluginNavigationGroup(String owner, JPanel panel) {
			this.owner = owner;
			this.panel = panel;
		}
	}

    private UiContributionSpec findView(UiContributionSpec navigation, List<UiContributionSpec> views) {
		for (UiContributionSpec view : views) {
			if (view.owner().equals(navigation.owner()) && view.id().equals(navigation.target())) return view;
		}
		return null;
    }

    public void setFooterAction(String text, String command, ActionListener listener) {
		setFooterAction(text, null, command, listener);
	}

	public void setFooterAction(String text, Icon icon, String command, ActionListener listener) {
        footerActions.removeAll();
        footerButton = new JButton(text);
		footerButton.setIcon(icon);
		footerButton.setIconTextGap(10);
        footerButton.setActionCommand(command);
        footerButton.addActionListener(listener);
        CompanywareTheme.navigationButton(footerButton);
        footerButton.setPreferredSize(new Dimension(176, 46));
        footerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        footerActions.add(footerButton);
        revalidate();
        repaint();
    }

    public void addFooterAction(String text, Icon icon, String command, ActionListener listener) {
        JButton button = new JButton(text, icon);
        button.setIconTextGap(10); button.setActionCommand(command); button.addActionListener(listener);
        CompanywareTheme.navigationButton(button);
        button.setPreferredSize(new Dimension(176, 42));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        footerActions.add(button); revalidate(); repaint();
        if ("logout".equals(command)) logoutFooterButton = button;
    }

    public void setFooterText(String text) { if (footerButton != null) footerButton.setText(text); }
    public void setLogoutFooterText(String text) { if (logoutFooterButton != null) logoutFooterButton.setText(text); }

    public void refreshTexts() {
        TextMessages service = ApplicationContextProvider.getContext().getBean(TextMessages.class);
        for (Object value : bars.values()) {
            BarInfo info = (BarInfo) value;
            info.getButton().setText(info.displayName(service));
        }
        for (Object value : barIconEntries.values()) {
            JLabel label = (JLabel) value;
            label.setText(service.get(label.getName()));
        }
        revalidate(); repaint();
    }

    public void setSelectedEntry(String entryName) {
        selectedEntry = entryName;
		if (entryName != null && !entryName.isEmpty()) {
			currentSdkView = null;
			activeNavigationChild = null;
			BarInfo visible = visibleBarInfo();
			if (visible != null && !visible.isHeading()) {
				visibleBar = -1;
				render();
			}
			refreshActiveNavigationChild();
		}
        for (Object value : barIconEntries.values()) {
            JLabel label = (JLabel) value;
            boolean selected = label.getName().equals(entryName);
            label.setOpaque(selected);
            label.setForeground(CompanywareTheme.text());
            label.setBackground(selected ? CompanywareTheme.navigationSelected() : CompanywareTheme.sidebar());
            label.setBorder(selected ? CompanywareTheme.selectedNavigationBorder()
                    : BorderFactory.createEmptyBorder(8, 6, 8, 6));
        }
        if (footerButton != null) {
            boolean selected = footerButton.getActionCommand().equals(entryName);
            footerButton.setForeground(CompanywareTheme.text());
            footerButton.setBackground(selected ? CompanywareTheme.navigationSelected() : CompanywareTheme.surface());
            footerButton.setBorder(selected ? CompanywareTheme.selectedNavigationBorder()
                    : BorderFactory.createEmptyBorder(12, 16, 12, 16));
        }
        repaint();
    }

	private BarInfo visibleBarInfo() {
		int index = 0;
		for (Object value : bars.values()) {
			if (index++ == visibleBar) return (BarInfo) value;
		}
		return null;
	}

    public String getSelectedEntry() { return selectedEntry; }

    public void applyTheme() {
		setBackground(CompanywareTheme.sidebar());
		topPanel.setBackground(CompanywareTheme.sidebar());
		bottomPanel.setBackground(CompanywareTheme.sidebar());
		southPanel.setBackground(CompanywareTheme.sidebar());
		footerActions.setBackground(CompanywareTheme.sidebar());
		for (Object value : bars.values()) {
			BarInfo info = (BarInfo) value;
			info.applyStyle();
			info.getComponent().setBackground(CompanywareTheme.sidebar());
		}
		setSelectedEntry(selectedEntry == null ? "" : selectedEntry);
		refreshActiveNavigationChild();
	}

	public void clearSessionState() {
		selectedEntry = null;
		activeNavigationChild = null;
		refreshActiveNavigationChild();
		setSelectedEntry("");
	}

	public void setNavigationEnabled(boolean enabled) {
		setEnabledRecursively(this, enabled);
	}

	private static void setEnabledRecursively(Component component, boolean enabled) {
		component.setEnabled(enabled);
		if (component instanceof Container) for (Component child : ((Container) component).getComponents()) setEnabledRecursively(child, enabled);
	}

    /**
     * Removes the specified bar from the JOutlookBar
     * 
     * @param  name  The name of the bar to remove
     */
    public void removeBar(String name) {
        this.bars.remove(name);
        render();
    }

    /**
     * Returns the index of the currently visible bar (zero-based)
     * 
     * @return The index of the currently visible bar
     */
    public int getVisibleBar() {
        return this.visibleBar;
    }

    /**
     * Programmatically sets the currently visible bar; the visible bar
     * index must be in the range of 0 to size() - 1
     * 
     * @param  visibleBar   The zero-based index of the component to make visible
     */
    public void setVisibleBar(int visibleBar) {
        if (visibleBar > 0
                && visibleBar < this.bars.size()) {
            this.visibleBar = visibleBar;
            render();
        }
    }
    
    public int getBarSize(){
    	return this.bars.size();
    }
    
    public int getBarEntrySize(){
    	return this.barIconEntries.size();
    }

    /**
     * Causes the outlook bar component to rebuild itself; this means that
     * it rebuilds the top and bottom panels of bars as well as making the
     * currently selected bar's panel visible
     */
    public void render() {
        // Compute how many bars we are going to have where
        int totalBars = this.bars.size();
		if (totalBars == 0) {
			topPanel.removeAll();
			bottomPanel.removeAll();
			if (visibleComponent != null) remove(visibleComponent);
			visibleComponent = null;
			revalidate();
			repaint();
			return;
		}
		if (visibleBar >= totalBars) visibleBar = -1;
        int topBars = totalBars;
        int bottomBars = 0;
        
        // Ordering JOutlookbar by priority
        this.sortByPriority();
        
        // Get an iterator to walk through out bars with
        Iterator itr = this.bars.keySet().iterator();

        // Render the top bars: remove all components, reset the GridLayout to
        // hold to correct number of bars, add the bars, and "validate" it to
        // cause it to re-layout its components
        this.topPanel.removeAll();
		List<BarInfo> orderedBars = new java.util.ArrayList<BarInfo>();
		for (Object value : this.bars.values()) orderedBars.add((BarInfo) value);
		BarInfo selectedBarInfo = null;
        for (int i = 0; i < topBars; i++) {
            String barName = (String) itr.next();
            BarInfo barInfo = (BarInfo) this.bars.get(barName);
			JButton button = barInfo.getButton();
			boolean expanded = i == visibleBar && barInfo.isHeading();
			button.putClientProperty("companyware.navigation.expanded", Boolean.valueOf(expanded));
			button.putClientProperty("companyware.navigation.active", Boolean.valueOf(i == visibleBar));
			button.setBackground(i == visibleBar
					? CompanywareTheme.navigationSelected() : CompanywareTheme.sidebar());
			button.repaint();
			button.setAlignmentX(Component.LEFT_ALIGNMENT);
			button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
			this.topPanel.add(button);
			if (i == visibleBar) {
				if (barInfo.isHeading()) {
					JComponent groupPanel = barInfo.getComponent();
					groupPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
					groupPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
							groupPanel.getPreferredSize().height));
					this.topPanel.add(groupPanel);
				} else {
					selectedBarInfo = barInfo;
				}
			}
        }
        this.topPanel.revalidate();
        this.topPanel.repaint();


        // Render the center component: remove the current component (if there
        // is one) and then put the visible component in the center of this panel
        if (this.visibleComponent != null) {
            this.remove(this.visibleComponent);
        }
		this.visibleComponent = selectedBarInfo == null ? null : selectedBarInfo.getComponent();
		if (visibleComponent != null) this.add(visibleComponent, BorderLayout.CENTER);

        // Render the bottom bars: remove all components, reset the GridLayout to
        // hold to correct number of bars, add the bars, and "validate" it to
        // cause it to re-layout its components
        this.bottomPanel.removeAll();
        GridLayout bottomLayout = (GridLayout) this.bottomPanel.getLayout();
        bottomLayout.setRows(bottomBars);
        for (int i = 0; i < bottomBars; i++) {
            String barName = (String) itr.next();
            BarInfo barInfo = (BarInfo) this.bars.get(barName);
            JButton button = barInfo.getButton();
            this.bottomPanel.add(button);
        }
        this.bottomPanel.revalidate();
        this.bottomPanel.repaint();

        // Validate all of our components: cause this container to re-layout its subcomponents
        this.revalidate();
        this.repaint();
        if (getParent() != null) {
            try {
                //try to refit size if parent is a scrollpane's viewport (used for fast options in DSWorkbenchMainFrame)
                getParent().revalidate();
                getParent().repaint();
            } catch (Exception ignored) {
            }
        }
    }
    
    private void sortByPriority() {
        List<Map.Entry<String, BarInfo>> list = new LinkedList<Map.Entry<String, BarInfo>>(this.bars.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, BarInfo>>() {
            public int compare(Map.Entry<String, BarInfo> o1, Map.Entry<String, BarInfo> o2) {
                return (Integer.valueOf(o1.getValue().getPriority())).compareTo(Integer.valueOf(o2.getValue().getPriority()));
            }
        });

        /// Loop the sorted list and put it into a new insertion order Map
        /// LinkedHashMap
        this.bars = new LinkedHashMap<String, BarInfo>();
        for (Map.Entry<String, BarInfo> entry : list) {
            this.bars.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Invoked when one of our bars is selected
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        int currentBar = 0;
        for (Object o : this.bars.keySet()) {
            String barName = (String) o;
            BarInfo barInfo = (BarInfo) this.bars.get(barName);
            if (barInfo.getButton() == e.getSource()) {
                // Found the selected button
				this.visibleBar = barInfo.isHeading() && this.visibleBar == currentBar ? -1 : currentBar;
				if (barInfo.isHeading()) {
					setSelectedEntry("");
					currentSdkView = null;
					activeNavigationChild = null;
				}
                render();
				refreshActiveNavigationChild();
				if (barInfo.getAction() != null) barInfo.getAction().run();
                return;
            }
            currentBar++;
        }
    }
    
    public JPanel getPanel(String entryName){
    	Iterator<Map.Entry<String, LinkedHashMap>> labels = ((Map) this.barEntries.get(entryName)).entrySet().iterator();
    	JPanel panel = null;
    	while (labels.hasNext()) {
    	    Map.Entry<String, LinkedHashMap> entry = labels.next();
    	    String key = entry.getKey();
    	    Map value = entry.getValue();
    	   
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(CompanywareTheme.sidebar());
    	    Iterator<Map.Entry<String, JLabel>> entries = value.entrySet().iterator();
    	    while (entries.hasNext()) {
	      	  Map.Entry<String, JLabel> innerEntry = entries.next();
	      	  String labelKey = innerEntry.getKey();
	      	  JLabel label = innerEntry.getValue();
			JLabel item = (JLabel) label;
			item.setAlignmentX(Component.CENTER_ALIGNMENT);
			panel.add(item);
			panel.add(Box.createVerticalStrut(6));
	      	}
    	}
    	
    	return panel;	
    }

    /**
     * Debug, dummy method
     */
    public static JPanel getDummyPanel(String name) {
        JPanel panel = new JPanel(new GridLayout(3,1));
   //     panel.setPreferredSize(new Dimension(100,200));
        panel.setBackground(Color.green);
        panel.setBorder(BorderFactory.createLineBorder(Color.black));
        panel.add(new JLabel(name, JLabel.CENTER));
        panel.add(new JLabel(name+"2", JLabel.CENTER));
        panel.add(new JLabel(name+"3", JLabel.CENTER));
        return panel;
    }

    /**
     * Internal class that maintains information about individual Outlook bars;
     * specifically it maintains the following information:
     * 
     * name      The name of the bar
     * button     The associated JButton for the bar
     * component    The component maintained in the Outlook bar
     */
    class BarInfo{

        /**
         * The name of this bar
         */
        private String name;
        /**
         * The JButton that implements the Outlook bar itself
         */
        private JButton button;
        /**
         * The component that is the body of the Outlook bar
         */
        private JComponent component;
        
        private int priority = 1;
		private Runnable action;
		private boolean localized = true;
		private java.util.function.Supplier<String> titleSupplier;
		private boolean heading;

		/**
         * Creates a new BarInfo
         * 
         * @param  name    The name of the bar
         * @param  component  The component that is the body of the Outlook Bar
         */
        public BarInfo(String name, JComponent component) {
            this.name = name;
            this.component = component;
            
            if(component!=null){
	            Dimension componentDimension = this.component.getPreferredSize();
	            componentDimension.height = 150;
	            this.component.setPreferredSize(componentDimension);
            }
            TextMessages service = ApplicationContextProvider.getContext().getBean(TextMessages.class);
            this.button = new JButton(service.get(name));
			CompanywareTheme.navigationButton(this.button);
            Dimension buttonDimension = this.button.getPreferredSize();
            buttonDimension.height = 50;
            this.button.setPreferredSize(buttonDimension);
        }

		/**
         * Creates a new BarInfo
         * 
         * @param  name    The name of the bar
         * @param  component  The component that is the body of the Outlook Bar
         */
        public BarInfo(String name, JComponent component, int priority) {
            this.name = name;
            this.component = component;
            this.priority = priority;
            
            if(component!=null){
	            Dimension componentDimension = this.component.getPreferredSize();
	            componentDimension.height = 150;
	            this.component.setPreferredSize(componentDimension);
            }
            TextMessages service = ApplicationContextProvider.getContext().getBean(TextMessages.class);
            this.button = new JButton(service.get(name));
			CompanywareTheme.navigationButton(this.button);
            Dimension buttonDimension = this.button.getPreferredSize();
            buttonDimension.height = 50;
            this.button.setPreferredSize(buttonDimension);
        }

		public BarInfo(String name, JComponent component, int priority, Runnable action, boolean localized) {
			this(name, component, priority, action, localized, false);
		}

		public BarInfo(String name, JComponent component, int priority, Runnable action, boolean localized,
				boolean heading) {
			this.name = name;
			this.component = component;
			this.priority = priority;
			this.action = action;
			this.localized = localized;
			this.heading = heading;
			this.button = new NavigationButton(heading);
			this.button.setText(localized
					? ApplicationContextProvider.getContext().getBean(TextMessages.class).get(name) : name);
			if (heading) this.button.putClientProperty("companyware.navigation.group", Boolean.TRUE);
			applyStyle();
		}

		private final class NavigationButton extends JButton {
			private final boolean group;

			private NavigationButton(boolean group) { this.group = group; }

			@Override protected void paintComponent(java.awt.Graphics graphics) {
				super.paintComponent(graphics);
				java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
				try {
					g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
							java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
					g.setRenderingHint(java.awt.RenderingHints.KEY_STROKE_CONTROL,
							java.awt.RenderingHints.VALUE_STROKE_PURE);
					g.setColor(getForeground());
					float scale = Math.max(1f, Math.min(getWidth(), getHeight()) / 50f);
					g.setStroke(new java.awt.BasicStroke(1.75f * scale, java.awt.BasicStroke.CAP_ROUND,
							java.awt.BasicStroke.JOIN_ROUND));
					float x = getWidth() - (22f * scale), y = getHeight() / 2f;
					if (group) {
						boolean open = Boolean.TRUE.equals(getClientProperty("companyware.navigation.expanded"));
						float direction = open ? -1f : 1f;
						java.awt.geom.Path2D.Float path = new java.awt.geom.Path2D.Float();
						path.moveTo(x - 4f * scale, y - 3f * direction * scale);
						path.lineTo(x, y + 2f * direction * scale);
						path.lineTo(x + 4f * scale, y - 3f * direction * scale);
						g.draw(path);
					} else {
						java.awt.geom.Path2D.Float path = new java.awt.geom.Path2D.Float();
						path.moveTo(x - 3f * scale, y - 4f * scale);
						path.lineTo(x + 2f * scale, y);
						path.lineTo(x - 3f * scale, y + 4f * scale);
						g.draw(path);
					}
				} finally { g.dispose(); }
			}
		}

		private void applyStyle() {
			CompanywareTheme.navigationButton(this.button);
			this.button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
			this.button.setRolloverEnabled(true);
			this.button.setFocusPainted(true);
			this.button.setPreferredSize(new Dimension(this.button.getPreferredSize().width, 50));
		}

		public boolean isHeading() { return heading; }

        /**
         * Creates a new BarInfo
         * 
         * @param  name    The name of the bar
         * @param  icon    JButton icon
         * @param  component  The component that is the body of the Outlook Bar
         */
        public BarInfo(String name, Icon icon, JComponent component) {
            this.name = name;
            this.component = component;
            this.button = new JButton(name, icon);
			CompanywareTheme.navigationButton(this.button);
        }

        /**
         * Returns the name of the bar
         * 
         * @return The name of the bar
         */
        public String getName() {
            return this.name;
        }

        /**
         * Sets the name of the bar
         * 
         * @param  name The name of the bar
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Returns the outlook bar JButton implementation
         * 
         * @return   The Outlook Bar JButton implementation
         */
        public JButton getButton() {
            return this.button;
        }

        /**
         * Returns the component that implements the body of this Outlook Bar
         * 
         * @return The component that implements the body of this Outlook Bar
         */
        public JComponent getComponent() {
            return this.component;
        }
        
        public int getPriority() {
			return priority;
		}

		public void setPriority(int priority) {
			this.priority = priority;
		}

		public Runnable getAction() { return action; }
		public boolean isLocalized() { return localized; }
		public void setTitleSupplier(java.util.function.Supplier<String> titleSupplier) {
			this.titleSupplier = titleSupplier;
		}
		public String displayName(TextMessages messages) {
			if (titleSupplier != null) return titleSupplier.get();
			return localized ? messages.get(name) : name;
		}
	}
}
