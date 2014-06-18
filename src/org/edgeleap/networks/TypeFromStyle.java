package org.edgeleap.networks;

import java.awt.Color;

import org.pathvisio.core.model.LineStyle;
import org.pathvisio.core.model.LineType;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.view.MIMShapes;

// Derives interaction type from arrow style
public class TypeFromStyle {
		PathwayElement pe;
		
		public TypeFromStyle(PathwayElement line) {
			pe = line;
		}
		
		public InteractionType getType() {
			InteractionType type = null;
			
			if(isTransport()) type = InteractionType.TRANSPORT;
			if(isStimulation()) type = InteractionType.STIMULATION;
			if(isBinding()) type = InteractionType.BINDING;
			if(isCleavage()) type = InteractionType.CLEAVAGE;
			if(isComposedOf()) type = InteractionType.COMPOSED_OF;
			if(isReceptorBinding()) type = InteractionType.RECEPTOR_BINDING;
			if(isInhibition()) {
				type = InteractionType.INHIBITION;
				if(isNegativeCorrelation()) type = InteractionType.NEGATIVE_CORRELATION;
			}
			if(isPositiveCorrelation()) type = InteractionType.POSITIVE_CORRELATION;
			
			return type;
		}
		
		public boolean isDirectedForward() {
			return
				LineType.LINE.equals(pe.getStartLineType()) &&
				!LineType.LINE.equals(pe.getEndLineType());
		}
		
		public boolean isDirectedBackward() {
			return
				LineType.LINE.equals(pe.getEndLineType()) &&
				!LineType.LINE.equals(pe.getStartLineType());
		}
		
		public boolean isTransport() {
			// Transport: mim-conversion head, color 255,0,112
			return 
				pe.getEndLineType() == MIMShapes.MIM_CONVERSION &&
				new Color(255, 0, 112).equals(pe.getColor());
		}
		
		public boolean isInhibition() {
			return 
				pe.getEndLineType() == MIMShapes.MIM_INHIBITION ||
				pe.getEndLineType() == LineType.TBAR;
		}
		
		public boolean isFilledArrow() {
			return 
				pe.getEndLineType() == MIMShapes.MIM_CONVERSION ||
				pe.getEndLineType() == LineType.ARROW;
		}
		
		public boolean isNegativeCorrelation() {
			// Negative correlation: tbar head, color 0,0,255
			return 
				isInhibition() &&
					new Color(0, 0, 255).equals(pe.getColor());
		}
		
		public boolean isPositiveCorrelation() {
			// Positive correlation: filled arrow head, color 0,0,255
			return 
				isFilledArrow() &&
					new Color(0, 0, 255).equals(pe.getColor());
		}
		
		public boolean isStimulation() {
			return 
				pe.getEndLineType() == MIMShapes.MIM_STIMULATION;
		}
		
		public boolean isBinding() {
			return 
				pe.getEndLineType() == MIMShapes.MIM_BINDING;
		}
		
		public boolean isCleavage() {
			return 
				pe.getEndLineType() == MIMShapes.MIM_CLEAVAGE;
		}
		
		public boolean isNegativeEffect() {
			// Negative effect: style dashed, color 255,153,0
			return 
				pe.getLineStyle() == LineStyle.DASHED &&
				new Color(255, 153, 0).equals(pe.getColor());
		}
		
		public boolean isComposedOf() {
			// Composed of: style dashed, color 255,153,255
			return 
				pe.getLineStyle() == LineStyle.DASHED &&
				new Color(255, 153, 255).equals(pe.getColor());
		}
		
		public boolean isReceptorBinding() {
			// Receptor: end style receptor / ligand
			return 
				pe.getEndLineType() == LineType.RECEPTOR ||
				pe.getEndLineType() == LineType.RECEPTOR_ROUND ||
				pe.getEndLineType() == LineType.RECEPTOR_SQUARE ||
				pe.getEndLineType() == LineType.LIGAND_ROUND ||
				pe.getEndLineType() == LineType.LIGAND_SQUARE;
		}
}
