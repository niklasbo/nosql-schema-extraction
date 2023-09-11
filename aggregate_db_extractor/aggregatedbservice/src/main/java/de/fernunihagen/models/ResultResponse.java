package de.fernunihagen.models;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ResultResponse {
    private Object jsonSchema;

    public ResultResponse() {
    }

    public ResultResponse(Object jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((jsonSchema == null) ? 0 : jsonSchema.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ResultResponse other = (ResultResponse) obj;
        if (jsonSchema == null) {
            if (other.jsonSchema != null)
                return false;
        } else if (!jsonSchema.equals(other.jsonSchema))
            return false;
        return true;
    }

    public Object getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(Object jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

}
