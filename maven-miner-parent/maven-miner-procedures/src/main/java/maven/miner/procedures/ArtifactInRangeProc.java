package maven.miner.procedures;

import java.time.LocalDate;
import java.util.stream.Stream;

//import org.neo4j.graphdb.Direction;
//import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
//import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import fr.inria.diverse.maven.common.DependencyRelation;
import fr.inria.diverse.maven.common.Properties;
import maven.miner.output.OutputNode;

public class ArtifactInRangeProc extends AbstractProcedureEnv {
	
	/**
	 * This procedure returns all latest artifacts
	 */
	@Procedure(value="maven.miner.artifacts.during", mode = Mode.READ)
	@Description("retrieve all latest artifacts in a particular range")
	public Stream<OutputNode> getArtifactsInRange(@Name("Deployment year") long year, 
			  									  @Name(value= "Deployment month", defaultValue = "0") long month) 
	{		
		return getArtifactsInRange(Properties.ARTIFACT_LABEL, year, month);
	}
	
	/**
	 * 
	 * This procedure returns all latest artifacts
	 */
	@Procedure(value="maven.miner.artifacts.group.during", mode = Mode.READ)
	@Description("retrieve all latest artifacts in a particular range")
	public Stream<OutputNode> getArtifactsInRange(@Name(value = "Group name") String groupName,
												  @Name("Deployment year") long year, 
												  @Name(value= "Deployment month", defaultValue = "0") long month) 
	{	
		Stream<OutputNode> result = null;
		try (Transaction tx = graphDB.beginTx()) {
			String query = "";
			if (month == 0) {
					query = String.format("match (n:`%s`)"
												+ "-[c:%s]->(y:%s {%s : %d}) "
											  	+ "return n", 
												groupName, 
												DependencyRelation.YEAR.toString(),
												Properties.CALENDAR_LABEL,
												Properties.YEAR,
												year);
			} else {
					query = String.format("match (m:%s {%s : %d})<-[:%s]-(n:`%s`)"
												+ "-[c:%s]->(y:%s {%s : %d}) "
											  	+ "return n", 
												DependencyRelation.MONTH.toString(),
												Properties.CALENDAR_LABEL,
												Properties.MONTH,
												month,
												groupName, 
												DependencyRelation.YEAR.toString(),
												Properties.CALENDAR_LABEL,
												Properties.YEAR,
												year);
			}
			Result queryResult = graphDB.execute(query);
			result = queryResult.columnAs("n")
								.stream()
								.map(node -> new OutputNode((Node)node));
			tx.success();
		} catch (Exception e) {
			log.error(e.getMessage());
			throw e;	
		}
		return result;
	}
	
	/**
	 * 
	 * This procedure returns all latest artifacts
	 */
	@Procedure(value="maven.miner.artifacts.between", mode = Mode.READ)
	@Description("retrieve all latest artifacts in a particular range")
	public Stream<OutputNode> getArtifactsInPeriod(
			@Name("start date") LocalDate startDate, 
			@Name(value= "end date", defaultValue = "9999-12-31") LocalDate endDate) 
	{
		return getArtifactsInPeriod(Properties.ARTIFACT_LABEL, startDate, endDate);
	}
	/**
	 * This procedure returns all latest artifacts
	 */
	@Procedure(value="maven.miner.artifacts.group.between", mode = Mode.READ)
	@Description("retrieve all latest artifacts in a particular range")
	public Stream<OutputNode> getArtifactsInPeriod(@Name(value = "Group name") String groupName,
												   @Name("start date") LocalDate startDate, 
												   @Name(value= "end date", defaultValue = "9999-12-31") LocalDate endDate) 
	{
		int startYear = startDate.getYear();
		int startMonth = startDate.getMonthValue();
		
		int endYear = endDate.getYear();
		int endMonth = endDate.getMonthValue();
		
		Stream<OutputNode> result = null;
		try (Transaction tx = graphDB.beginTx()) {
			String query = String.format("match (m)<-[:MONTH]-(n:`%s`)-[:YEAR]->(y) \n" + 
											"where (y.year = %d and m.month > %d) \n" + 
											"or y.year> %d\n" + 
											"or y.year< %d \n" + 
											"or (y.year = %d and m.month < %d)\n" + 
											"return n",
											groupName,
											startYear,
											startMonth,
											startYear,
											endYear,
											endYear,
											endMonth);
			
			Result queryResult = graphDB.execute(query);
			result = queryResult.columnAs("n")
								.stream()
								.map(node -> new OutputNode((Node)node));
			tx.success();
		} catch (Exception e) {
			log.error(e.getMessage());
			throw e;	
		}
		return result;
	}
	
	
	@SuppressWarnings("unused")
	private class YearMonthEvaluator implements Evaluator {

		@Override
		public Evaluation evaluate(Path path) {
			boolean continues = false;
			boolean includes = false;
     		return Evaluation.of(includes, continues);
		}
		
	}

}

// The traversal API is supposed to be faster then CypHer QUERIES, HOWEVER, NOT BENIFITING FROM ACCESS TO INDEX MAY SLOW DOWN THE QUERY
// Retrieving the calendar node corresponding to the appropriate year 
//Node yearNode = graphDB.findNode(calendarLabel, Properties.YEAR, year);

//if (yearNode == null) return Stream.empty();
//result = graphDB .traversalDescription()
//		.depthFirst()
//		.evaluator(path -> { return Evaluation.of(path.endNode().hasLabel(Label.label(groupName)), false);})
//		.relationships(DependencyRelation.YEAR, Direction.INCOMING)
//		.uniqueness(Uniqueness.NODE_GLOBAL)
//		.traverse(yearNode)
//		.nodes()
//		.stream()
//

